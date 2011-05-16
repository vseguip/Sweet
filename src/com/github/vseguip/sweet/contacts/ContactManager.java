/********************************************************************\

File: ContactManager.java
Copyright 2011 Vicent Seguí Pascual

This file is part of Sweet.  Sweet is free software: you can
redistribute it and/or modify it under the terms of the GNU General
Public License as published by the Free Software Foundation, either
version 3 of the License, or (at your option) any later version.

Sweet is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
for more details.  You should have received a copy of the GNU General
Public License along with Sweet. 

If not, see http://www.gnu.org/licenses/.  
\********************************************************************/

package com.github.vseguip.sweet.contacts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.github.vseguip.sweet.R;
import com.github.vseguip.sweet.rest.SweetContact;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts.Data;
import android.provider.ContactsContract.RawContacts.Entity;
import android.text.TextUtils;
import android.util.Log;

public class ContactManager {
	private static final String TAG = "ContactManager";
	private static final String ACCOUNT_VERSION = "0.1";
	private static String ACCOUNT_TYPE;
	private static final String[] RAW_CONTACT_ID_PROJECTION = { RawContacts._ID };
	private static String SOURCE_ID_QUERY = RawContacts.ACCOUNT_TYPE + "=? AND " + RawContacts.SOURCE_ID + "=?";

	private static final String[] DATA_ID_PROJECTION = { ContactsContract.Data._ID };
	private static String FIELD_ID_QUERY = ContactsContract.Data.RAW_CONTACT_ID + "=? AND "
			+ ContactsContract.Data.SYNC1 + "=?";

	// Gets all dirty contacts from an account
	private static String DIRTY_CONTACT_QUERY = RawContacts.ACCOUNT_TYPE + "=? AND " + RawContacts.ACCOUNT_NAME
			+ "=? AND " + RawContacts.DIRTY + "=1" + " AND " + RawContacts.SOURCE_ID + " IS NOT NULL";

	// Gets all dirty contacts from an account
	private static String NEW_LOCAL_CONTACT_QUERY = RawContacts.ACCOUNT_TYPE + "=? AND " + RawContacts.ACCOUNT_NAME
			+ "=? AND " + RawContacts.DIRTY + "=1" + " AND " + RawContacts.SOURCE_ID + " IS NULL";

	private static class ContactFields {
		private static String MODIFIED_DATE_COLUMN = RawContacts.SYNC2;
		private static String VERSION_COLUMN = RawContacts.SYNC3;
		private static String[] FIELDS = { ISweetContact.FIRST_NAME_KEY, ISweetContact.ACCOUNT_NAME_KEY,
				ISweetContact.EMAIL1_KEY, ISweetContact.WORK_PHONE_KEY, ISweetContact.MOBILE_PHONE_KEY,
				ISweetContact.WORK_FAX_KEY, ISweetContact.CITY_KEY };

		private static String[] MIMETYPE_KEYS = { StructuredName.MIMETYPE, Organization.MIMETYPE, Email.MIMETYPE,
				Phone.MIMETYPE, Phone.MIMETYPE, Phone.MIMETYPE, StructuredPostal.MIMETYPE };
		private static String[] MIMETYPES = { StructuredName.CONTENT_ITEM_TYPE, Organization.CONTENT_ITEM_TYPE,
				Email.CONTENT_ITEM_TYPE, Phone.CONTENT_ITEM_TYPE, Phone.CONTENT_ITEM_TYPE, Phone.CONTENT_ITEM_TYPE,
				StructuredPostal.CONTENT_ITEM_TYPE };
		private static String[] DATA_KEYS = { StructuredName.GIVEN_NAME, Organization.COMPANY, Email.DATA,
				Phone.NUMBER, Phone.NUMBER, Phone.NUMBER, StructuredPostal.CITY };
		private static String[] TYPE_KEYS = { null, null, Email.TYPE, Phone.TYPE, Phone.TYPE, Phone.TYPE,
				StructuredPostal.TYPE };
		private static Integer[] TYPES = { null, null, Email.TYPE_WORK, Phone.TYPE_WORK, Phone.TYPE_MOBILE,
				Phone.TYPE_FAX_WORK, StructuredPostal.TYPE_WORK };
		private static String[][] EXTRA_KEYS = {
				{ StructuredName.FAMILY_NAME },
				{ Organization.TITLE, Organization.SYNC2 },
				null,
				null,
				null,
				null,
				{ StructuredPostal.STREET, StructuredPostal.COUNTRY, StructuredPostal.POSTCODE, StructuredPostal.REGION } };
		private static String[][] EXTRA_FIELDS = {
				{ ISweetContact.LAST_NAME_KEY },
				{ ISweetContact.TITLE_KEY, ISweetContact.ACCOUNT_ID_KEY },
				null,
				null,
				null,
				null,
				{ ISweetContact.STREET_KEY, ISweetContact.COUNTRY_KEY, ISweetContact.POSTAL_CODE_KEY,
						ISweetContact.STATE_KEY }, null };
	}

	/**
	 * Syncs a list of contacts obtained from the server. Uses batch operations
	 * for efficiency
	 * 
	 * @param contacts
	 *            A list of objects obtained from the Sugar Server implementing
	 *            the ISweetContact interface.
	 */

	public static int syncContacts(Context context, Account acc,
			List<ISweetContact> contacts) {
		Log.i(TAG, "syncContacts()");
		getAccountType(context);
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		ContentResolver resolver = context.getContentResolver();
		Log.i(TAG, "Starting to sync locally");
		int i = 0;
		try {
			for (ISweetContact c : contacts) {
				long localId = findLocalContact(resolver, c.getId());
				if (localId == 0) {
					addContact(resolver, ops, acc.name, c);
				} else {
					updateContact(resolver, ops, c, localId, true);
				}
				if ((ops.size() > 0) && (i % 50 == 0)) {
					Log.e(TAG, "Applying " + ops.size()
							+ " operations in a batch");
					resolver.applyBatch(ContactsContract.AUTHORITY, ops);
					ops.clear();
				}
				i++;
			}

			// Do the last pending ops
			if (ops.size() > 0) {
				Log.e(TAG, "Applying " + ops.size() + " operations in a batch");
				resolver.applyBatch(ContactsContract.AUTHORITY, ops);
				ops.clear();
			}

		} catch (RemoteException e) {
			Log.e(TAG, "Error applying the when syncing");
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			Log.e(TAG, "Error applying the when syncing");
			e.printStackTrace();
		}

		return 0;
	}

	/**
	 * Returns a list of contacts that have been marked as dirty and have a
	 * source ID (this means they are present in the server too).
	 * 
	 * @param context
	 *            Context calling the function
	 * @param account
	 *            Account to recover dirty contacts from
	 * @return List of contacts marked as dirty
	 */
	public static List<ISweetContact> getLocallyModifiedContacts(Context context, Account account) {

		String query_args[] = new String[] { getAccountType(context), account.name };
		List<ISweetContact> contacts = new ArrayList<ISweetContact>();
		ContentResolver res = context.getContentResolver();
		Cursor c = res.query(RawContacts.CONTENT_URI, new String[] { RawContacts._ID, RawContacts.SOURCE_ID,
				ContactFields.MODIFIED_DATE_COLUMN }, DIRTY_CONTACT_QUERY, query_args, null);
		try {
			if (c.moveToFirst()) {
				while (!c.isAfterLast()) {
					long rawId = c.getLong(0);
					SweetContact contact = new SweetContact();
					getContactData(res, rawId, contact);
					contact.setId(c.getString(1));
					contact.setDateModified(c.getString(2));
					contacts.add(contact);
					c.moveToNext();
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Exception querying dirty contacts" + e.getMessage());
			e.printStackTrace();
		} finally {
			c.close();
		}
		return contacts;
	}

	/**
	 * Returns a list of contacts that have been created in the phone and thus
	 * have no source ID (this means they have to be created in the server too).
	 * The Id of the contact will thus correspond to the rawID column.
	 * 
	 * @param context
	 *            Context calling the function
	 * @param account
	 *            Account to recover dirty contacts from
	 * @return List of contacts created locally and not yet in the server
	 */
	public static List<ISweetContact> getLocallyCreatedContacts(Context context, Account account) {

		String query_args[] = new String[] { getAccountType(context), account.name };
		List<ISweetContact> contacts = new ArrayList<ISweetContact>();
		ContentResolver res = context.getContentResolver();
		Cursor c = res.query(
								RawContacts.CONTENT_URI,
								RAW_CONTACT_ID_PROJECTION,
								NEW_LOCAL_CONTACT_QUERY,
								query_args,
								null);
		try {
			if (c.moveToFirst()) {
				while (!c.isAfterLast()) {
					long rawId = c.getLong(0);
					SweetContact contact = new SweetContact();
					getContactData(res, rawId, contact);
					if (contact.getId() == null) {// this should always be
						// null!!
						contact.setId(Long.toString(rawId));
						if ((contact.getFirstName() == null) || (contact.getLastName() == null)
								|| (!hasSugarProfileEntry(res, rawId))) {
							// this happens because we added the contact with
							// the
							// default android contact app that does not set the
							// sync fields properly
							// In this case we insert profile field and attempt
							// to recover
							// first and last name
							fixContactAdded(res, account, contact);
						}
						// add to list if we could fix it.
						if ((contact.getFirstName() != null) || (contact.getLastName() != null))
							contacts.add(contact);
					}
					c.moveToNext();
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Exception querying dirty contacts" + e.getMessage());
			e.printStackTrace();
		} finally {
			c.close();
		}
		return contacts;
	}

	/**
	 * Fixes the entry of a local contact that has not been created correctly.
	 * Will insert a profile entry and try to recover the name and set the
	 * appropiate sync fields for the StructuredName element
	 * 
	 * @param res
	 * @param contact
	 */
	private static void fixContactAdded(ContentResolver res, Account account, SweetContact contact) {

		long rawId = Long.parseLong(contact.getId());
		try {

			ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
			// Fix adding profile entry
			ContentProviderOperation.Builder builder;
			if (!hasSugarProfileEntry(res, rawId)) {
				if (rawId != 0) {
					builder = getDataInsertBuilder();
					buildMimeData(account.name, builder);
					builder.withValue(Data.RAW_CONTACT_ID, rawId);
					ops.add(builder.build());
				}
			}
			// Attempt to fix name entry
			long nameDataID = findNameDateId(res, rawId, contact);
			if (nameDataID != 0) {
				// add sync field for name!
				builder = getDataUpdateBuilder(nameDataID, true);
				builder.withValue(Data.SYNC1, ISweetContact.FIRST_NAME_KEY);
				ops.add(builder.build());
				try {
					res.applyBatch(ContactsContract.AUTHORITY, ops);
				} catch (RemoteException e) {
					Log.e(TAG, "Error trying to fix locally added contact");
					e.printStackTrace();
				} catch (OperationApplicationException e) {
					Log.e(TAG, "Error trying to fix locally added contact");
					e.printStackTrace();
				} catch (Exception e) {
					Log.e(TAG, "Error trying to fix locally added contact");
					e.printStackTrace();
				}

			}
		} catch (NumberFormatException n) {

		}
	}

	private static long findNameDateId(ContentResolver res, long rawId, SweetContact contact) {
		long dataId = 0;
		Cursor c = res.query(ContactsContract.Data.CONTENT_URI, new String[] { Data._ID, StructuredName.GIVEN_NAME,
				StructuredName.FAMILY_NAME }, Data.RAW_CONTACT_ID + "= ? AND " + Data.MIMETYPE + " = ?", new String[] {
				Long.toString(rawId), StructuredName.CONTENT_ITEM_TYPE }, null);
		try {
			if (c.moveToFirst()) {
				dataId = c.getLong(0);
				contact.setFirstName(c.getString(1));
				contact.setLastName(c.getString(2));
			}
		} catch (Exception ex) {

		} finally {
			c.close();
		}
		return dataId;
	}

	private static boolean hasSugarProfileEntry(ContentResolver res, long rawId) {
		boolean found = false;
		Cursor c = res.query(ContactsContract.Data.CONTENT_URI, new String[] { Data._ID }, Data.RAW_CONTACT_ID
				+ "= ? AND " + Data.MIMETYPE + " = ?", new String[] { Long.toString(rawId),
				"vnd.android.cursor.item/vnd.sweet.github.com.profile" }, null);
		found = (c.getCount() > 0);
		c.close();
		return found;
	}

	/**
	 * Tries to find a raw contact in the local database of the correct account
	 * type and with the same source ID
	 * 
	 * @param id
	 *            The remote UID of the contact
	 * @return The uid of the local raw contact
	 */
	public static long findLocalContact(ContentResolver resolver, String id) {
		String[] params = { getAccountType(), id };
		Cursor c = resolver.query(RawContacts.CONTENT_URI, RAW_CONTACT_ID_PROJECTION, SOURCE_ID_QUERY, params, null);
		// must be 1 and only 1!
		if (c.getCount() != 1) {
			c.close();
			return 0;
		}
		if (c.moveToFirst()) {
			long rawId = c.getLong(0);
			c.close();
			return rawId;// Return first column!
		}
		try {
			c.close();
		} catch (Exception ex) {
			Log.e(TAG, "Unexpected error closing cursor");
			ex.printStackTrace();
		}

		return 0;
	}

	/**
	 * Tries to find a data row in the local database for the specified sugar
	 * field and raw contact
	 * 
	 * @param id
	 *            The raw id of the contact
	 * @param field
	 *            The sugarCRM field we are interested in (stored in SYNC1)
	 * @return The uid of the local raw contact
	 */
	public static long findContactField(ContentResolver resolver, long rawId, String field) {
		String[] params = { Long.toString(rawId), field };
		Cursor c = resolver.query(ContactsContract.Data.CONTENT_URI, DATA_ID_PROJECTION, FIELD_ID_QUERY, params, null);
		if (c.moveToFirst()) {
			long dataId = c.getLong(0);
			c.close();
			return dataId;// Return first column!
		}
		try {
			c.close();
		} catch (Exception ex) {
			Log.e(TAG, "Unexpected error closing cursor");
			ex.printStackTrace();
		}

		return 0;
	}

	/**
	 * Save or update
	 * 
	 * @param context
	 * @param contact
	 * @param account
	 * @param create
	 */
	public static void createOrUpdateContact(Context context, ISweetContact contact, Account account, Uri entityUri,
			boolean create) {
		getAccountType(context);
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		if (contact == null)
			return;
		ContentResolver resolver = context.getContentResolver();
		if (create) {// new Contact (not from SugarCRM), save
			addContact(resolver, ops, account.name, contact);
		} else {// update contact in database
			long rawId = findLocalContact(resolver, contact.getId());
			if (rawId == 0) {
				// This means the contact has not yet been synced.
				// In such a case entityUri should
				// not be null (this is usually called by the
				// SweetContactEditorActivity) and should point to the raw
				// contact.
				if (entityUri != null) {
					rawId = getRawContactIdFromData(resolver, entityUri);
				}
			}
			if (rawId != 0) {
				updateContact(resolver, ops, contact, rawId, false);
			}
		}
		try {
			resolver.applyBatch(ContactsContract.AUTHORITY, ops);
		} catch (RemoteException e) {
			Log.e(TAG, "Error saving contact " + e.getMessage());
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			Log.e(TAG, "Error saving contact " + e.getMessage());
			e.printStackTrace();
		}

	}

	/**
	 * Adds a new contact to the database
	 * 
	 * @param resolver
	 *            The content resolver
	 * @param ops
	 *            The batch operations object
	 * @param contact
	 *            The contact we want to add
	 */
	private static void addContact(ContentResolver resolver, ArrayList<ContentProviderOperation> ops,
			String accountName, ISweetContact contact) {
		ContentValues values = new ContentValues();
		int reference = ops.size();
		// Basic data
		ContentProviderOperation.Builder builder = getRawContactInsertBuilder();
		values.put(RawContacts.SOURCE_ID, contact.getId());
		values.put(RawContacts.ACCOUNT_NAME, accountName);
		values.put(RawContacts.ACCOUNT_TYPE, getAccountType());
		values.put(ContactFields.VERSION_COLUMN, ACCOUNT_VERSION);
		values.put(ContactFields.MODIFIED_DATE_COLUMN, contact.getDateModified());
		builder.withValues(values);
		ops.add(builder.build());
		builder = getDataInsertBuilder();
		builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, reference);
		buildMimeData(accountName, builder);
		ops.add(builder.build());
		addContactData(ops, contact, values, reference);
	}

	/**
	 * @param accountName
	 * @param builder
	 */
	private static void buildMimeData(String accountName, ContentProviderOperation.Builder builder) {
		builder.withValue(ContactsContract.Data.MIMETYPE, "vnd.android.cursor.item/vnd.sweet.github.com.profile");
		builder.withValue(ContactsContract.Data.DATA1, accountName);
		builder.withValue(ContactsContract.Data.DATA2, "SugarCRM Profile");
		builder.withValue(ContactsContract.Data.DATA3, "Edit contact info");
	}

	/**
	 * Updates a contact in the database
	 * 
	 * @param resolver
	 *            The content resolver
	 * @param ops
	 *            The batch operations object
	 * @param contact
	 *            The contact we want to add
	 */
	private static void updateContact(ContentResolver resolver, ArrayList<ContentProviderOperation> ops,
			ISweetContact contact, long rawId, boolean sync) {
		ContentValues values = new ContentValues();
		if (sync) {
			// Reset the dirty flag for this contact if we are syncing and
			// update modified time
			ContentProviderOperation.Builder builder = getRawContactUpdateBuilder(rawId);
			values.put(RawContacts.DIRTY, 0);
			values.put(ContactFields.MODIFIED_DATE_COLUMN, contact.getDateModified());
			builder.withValues(values);
			ops.add(builder.build());
		}
		updateContactData(resolver, ops, contact, values, rawId, sync);
	}

	/**
	 * Add contact data to Data table
	 * 
	 * @param ops
	 *            batch operations
	 * @param contact
	 *            the contact
	 * @param values
	 *            A recycled values object
	 * @param reference
	 *            The back reference for the raw contact op
	 */
	private static void addContactData(ArrayList<ContentProviderOperation> ops, ISweetContact contact,
			ContentValues values, int reference) {
		for (int i = 0; i < ContactFields.FIELDS.length; i++) {
			try {
				String field = ContactFields.FIELDS[i];
				String mimetype_key = ContactFields.MIMETYPE_KEYS[i];
				String mimetype = ContactFields.MIMETYPES[i];
				String data_key = ContactFields.DATA_KEYS[i];
				String data = contact.get(field);
				String type_key = ContactFields.TYPE_KEYS[i];
				Integer type = ContactFields.TYPES[i];
				String[] extra_keys = ContactFields.EXTRA_KEYS[i];
				String[] extra_fields = ContactFields.EXTRA_FIELDS[i];
				values.clear();
				ContentProviderOperation.Builder builder = getDataInsertBuilder();
				if ((data_key != null) && (data != null) && !TextUtils.isEmpty(data)) {
					values.put(Data.SYNC1, field);// insert wich field generated
					// this row, will make it
					// easy to retrieve later
					// on!
					values.put(mimetype_key, mimetype);
					values.put(data_key, data);
					if (type_key != null)
						values.put(type_key, type);
					if (extra_keys != null) {
						for (int j = 0; j < extra_keys.length; j++)
							values.put(extra_keys[j], contact.get(extra_fields[j]));
					}
					builder.withValues(values);
					builder.withValueBackReference(Data.RAW_CONTACT_ID, reference);
					ops.add(builder.build());
				}

			} catch (ArrayIndexOutOfBoundsException ex) {
				Log.e(TAG, "Unknown error ocurred trying to get fields: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Updates contact data
	 * 
	 * @param ops
	 *            batch operations
	 * @param contact
	 *            the contact
	 * @param values
	 *            A recycled values object
	 * @param reference
	 *            The back reference for the raw contact op
	 */
	private static void updateContactData(ContentResolver resolver, ArrayList<ContentProviderOperation> ops,
			ISweetContact contact, ContentValues values, long rawId, boolean sync) {
		for (int i = 0; i < ContactFields.FIELDS.length; i++) {
			try {
				String field = ContactFields.FIELDS[i];
				String mimetype_key = ContactFields.MIMETYPE_KEYS[i];
				String mimetype = ContactFields.MIMETYPES[i];
				String data_key = ContactFields.DATA_KEYS[i];
				String data = contact.get(field);
				String type_key = ContactFields.TYPE_KEYS[i];
				Integer type = ContactFields.TYPES[i];
				String[] extra_keys = ContactFields.EXTRA_KEYS[i];
				String[] extra_fields = ContactFields.EXTRA_FIELDS[i];
				StringBuilder sb_query = new StringBuilder();
				ArrayList<String> sb_query_params = new ArrayList<String>();
				values.clear();
				if ((data_key != null) && (data != null) && !TextUtils.isEmpty(data)) {
					values.put(mimetype_key, mimetype);
					values.put(data_key, data);
					// Update only if the field is different, avoids unneeded
					// dirty marks and unneeded version increments
					sb_query.append("(").append(data_key).append(" <> ?)");
					sb_query_params.add(data);
					if (type_key != null)
						values.put(type_key, type);
					if (extra_keys != null) {
						for (int j = 0; j < extra_keys.length; j++) {
							values.put(extra_keys[j], contact.get(extra_fields[j]));
							// Update only if the field is different, avoids
							// unneeded dirty marks and unneeded version
							// increments
							sb_query.append(" OR (").append(extra_keys[j]).append(" <> ?)");
							sb_query_params.add(contact.get(extra_fields[j]));
						}
					}
				}
				long dataId = findContactField(resolver, rawId, field);
				if (dataId != 0) { // if the row exists, update it
					ContentProviderOperation.Builder builder = getDataUpdateBuilder(dataId, sync);
					if (values.size() > 0) {
						values.put(Data.SYNC1, field);// insert wich field
						// generated
						sb_query.append(" OR (").append(Data.SYNC1).append(" <> ?)");
						sb_query_params.add(field);
						builder.withValues(values);
						// Update only if the field is different, avoids
						// unneeded dirty marks and unneeded
						// version increments if we are not syncing
						if (!sync)
							builder.withSelection(sb_query.toString(), sb_query_params.toArray(new String[1]));
						ops.add(builder.build());
					}
				} else {
					// field didn't exists previously, insert it
					ContentProviderOperation.Builder builder = getDataInsertBuilder();
					if ((data_key != null) && (data != null) && !TextUtils.isEmpty(data)) {
						if (values.size() > 0) {
							values.put(Data.RAW_CONTACT_ID, rawId);
							values.put(Data.SYNC1, field);// insert wich field
							// generated
							builder.withValues(values);
							ops.add(builder.build());
							values.clear();
							builder = getRawContactUpdateBuilder(rawId);
							values.put(RawContacts.DIRTY, 1);
							builder.withValues(values);
							ops.add(builder.build());
						}

					}
				}

			} catch (ArrayIndexOutOfBoundsException ex) {
				Log.e(TAG, "Unknown error ocurred trying to get fields: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Get's a contact from the profile entry we encoded in addContact We first
	 * need to retrieve it's RawContactId then we use the getContactData call to
	 * set the data in the contact. Basically we are mirroring the
	 * addContactData methods
	 * 
	 * @param entityUri
	 *            : The uri to the entry in the Data table that encodes the
	 *            profile entry.
	 * @return an ISweetContact or null if some problem happened
	 */
	public static ISweetContact getContactFromMime(Context context, Uri entityUri) {

		SweetContact contact = null;
		ContentResolver res = context.getContentResolver();
		long rawId = getRawContactIdFromData(res, entityUri);
		if (rawId != 0) {
			contact = new SweetContact();
			getContactData(res, rawId, contact);

		}
		return contact;
	}

	/**
	 * Assigns a list of Id's generated by the SugarCRM to the local contact.
	 * This effectively binds the local contact with the remote one
	 * 
	 * @param context
	 *            Context of the caller
	 * @param contacts
	 *            List of contacts. The id field should correspond to the
	 *            RawContact Id
	 * @param newIds
	 *            List of Id's returned by SugarCRM. Must be of the same length
	 *            as the list of contacts
	 */

	public static void assignSourceIds(Context context, List<ISweetContact> contacts, List<String> newIds) {
		if (contacts.size() != newIds.size()) {
			// TODO: Something wrong happened should notify the user!
			return;
		}
		ContentResolver res = context.getContentResolver();
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		for (int i = 0; i < contacts.size(); i++) {
			ISweetContact contact = contacts.get(i);
			ContentProviderOperation.Builder builder = getRawContactUpdateBuilder(Long.parseLong(contact.getId()));
			builder.withValue(RawContacts.SOURCE_ID, newIds.get(i));
			ops.add(builder.build());
		}
		try {
			res.applyBatch(ContactsContract.AUTHORITY, ops);
			for (int i = 0; i < contacts.size(); i++) {
				contacts.get(i).setId(newIds.get(i));
			}
		} catch (RemoteException e) {
			Log.e(TAG, "Error assigning SOURCE_ID to local id " + e.getLocalizedMessage());
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			Log.e(TAG, "Error assigning SOURCE_ID to local id " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Cleans the dirty flag of a list of contacts.
	 * 
	 * @param context
	 *            Context of the caller
	 * @param contacts
	 *            List of contacts. The id field should correspond to the
	 *            RawContact Id
	 * 
	 */
	public static void cleanDirtyFlag(Context mContext, List<ISweetContact> contacts) {
		ContentResolver res = mContext.getContentResolver();
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		for (int i = 0; i < contacts.size(); i++) {
			ISweetContact contact = contacts.get(i);
			long rawId = findLocalContact(res, contact.getId());
			if (rawId != 0) {
				ContentProviderOperation.Builder builder = getRawContactUpdateBuilder(rawId);
				builder.withValue(RawContacts.DIRTY, "0");
				ops.add(builder.build());
			}
		}
		try {
			res.applyBatch(ContactsContract.AUTHORITY, ops);
		} catch (RemoteException e) {
			Log.e(TAG, "Error assigning SOURCE_ID to local id " + e.getLocalizedMessage());
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			Log.e(TAG, "Error assigning SOURCE_ID to local id " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	private static void getContactData(ContentResolver res, long rawId, ISweetContact contact) {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		for (int i = 0; i < ContactFields.FIELDS.length; i++) {
			String field = ContactFields.FIELDS[i];
			map.put(field, i);
		}
		Uri rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawId);
		Uri entityUri = Uri.withAppendedPath(rawContactUri, Entity.CONTENT_DIRECTORY);
		Cursor c = res.query(entityUri, null, null, null, null);
		try {
			if (c.moveToFirst()) {
				int index = c.getColumnIndex(Entity.SYNC1);
				while (!c.isAfterLast()) {
					if (TextUtils.isEmpty(contact.getId()) && (c.getColumnIndex(RawContacts.SOURCE_ID) != -1)) {
						contact.setId(c.getString(c.getColumnIndex(RawContacts.SOURCE_ID)));
					}
					String field = c.getString(index);
					if (map.containsKey(field)) {
						int i = map.get(field);
						String[] extra_keys = ContactFields.EXTRA_KEYS[i];
						String[] extra_fields = ContactFields.EXTRA_FIELDS[i];
						String data_key = ContactFields.DATA_KEYS[i];
						contact.set(field, c.getString(c.getColumnIndex(data_key)));
						if (extra_keys != null) {
							for (int j = 0; j < extra_keys.length; j++) {
								if (extra_keys[j] != null && extra_fields[j] != null) {
									contact.set(extra_fields[j], c.getString(c.getColumnIndex(extra_keys[j])));
								}
							}
						}
					}
					c.moveToNext();
				}
			}

		} catch (ArrayIndexOutOfBoundsException ex) {
			Log.e(TAG, "Unknown error ocurred trying to get fields: " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			c.close();
		}
	}

	/**
	 * Get's the RawContact Id from a Data table
	 * 
	 * @param res
	 *            The content resolver
	 * @param entityUri
	 *            The uri of the Data entry
	 * @return The Id of the rawContact
	 */
	private static long getRawContactIdFromData(ContentResolver res, Uri entityUri) {
		long rawId = 0;
		Cursor c = res.query(entityUri, new String[] { RawContacts.Data.RAW_CONTACT_ID }, null, null, null);
		try {
			while (c.moveToNext()) {
				rawId = c.getLong(0);
			}
		} finally {
			c.close();
		}
		return rawId;
	}

	/**
	 * 
	 */
	private static ContentProviderOperation.Builder getRawContactInsertBuilder() {
		return ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(RawContacts.CONTENT_URI))
				.withYieldAllowed(true);
	}

	private static ContentProviderOperation.Builder getRawContactUpdateBuilder(long rawId) {
		return ContentProviderOperation.newUpdate(
													addCallerIsSyncAdapterParameter(ContentUris
															.withAppendedId(RawContacts.CONTENT_URI, rawId)))
				.withYieldAllowed(true);
	}

	private static ContentProviderOperation.Builder getDataUpdateBuilder(long dataId, boolean syncAdapter) {
		if (syncAdapter) {
			return ContentProviderOperation.newUpdate(
														addCallerIsSyncAdapterParameter(ContentUris
																.withAppendedId(
																				ContactsContract.Data.CONTENT_URI,
																				dataId))).withYieldAllowed(true);
		} else {
			return ContentProviderOperation.newUpdate(
														ContentUris.withAppendedId(
																					ContactsContract.Data.CONTENT_URI,
																					dataId)).withYieldAllowed(true);

		}
	}

	private static ContentProviderOperation.Builder getDataInsertBuilder() {
		return ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI))
				.withYieldAllowed(true);
	}

	private static Uri addCallerIsSyncAdapterParameter(Uri uri) {
		return uri.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
	}

	private static String getAccountType() {
		return ACCOUNT_TYPE;
	}

	private static String getAccountType(Context context) {
		if (ACCOUNT_TYPE == null)
			ACCOUNT_TYPE = context.getString(R.string.account_type);
		return ACCOUNT_TYPE;
	}

}
