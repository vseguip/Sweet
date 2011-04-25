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
import java.util.List;

import com.github.vseguip.sweet.R;

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
import android.provider.ContactsContract.Contacts.Data;
import android.text.TextUtils;
import android.util.Log;

public class ContactManager {
	private static final String TAG = "ContactManager";
	private static String ACCOUNT_TYPE;
	private static final String[] RAW_CONTACT_ID_PROJECTION = { RawContacts._ID };
	private static String SOURCE_ID_QUERY = RawContacts.ACCOUNT_TYPE + "=? AND " + RawContacts.SOURCE_ID + "=?";

	private static final String[] DATA_ID_PROJECTION = { ContactsContract.Data._ID };
	private static String FIELD_ID_QUERY = ContactsContract.Data.RAW_CONTACT_ID + "=? AND "
			+ ContactsContract.Data.SYNC1 + "=?";

	private static class ContactFields {
		private static String[] FIELDS = { ISweetContact.FIRST_NAME_KEY, ISweetContact.ACCOUNT_NAME_KEY,
				ISweetContact.EMAIL1_KEY, ISweetContact.PHONE_WORK_KEY };

		private static String[] MIMETYPE_KEYS = { StructuredName.MIMETYPE, Organization.MIMETYPE, Email.MIMETYPE,
				Phone.MIMETYPE };
		private static String[] MIMETYPES = { StructuredName.CONTENT_ITEM_TYPE, Organization.CONTENT_ITEM_TYPE,
				Email.CONTENT_ITEM_TYPE, Phone.CONTENT_ITEM_TYPE };
		private static String[] DATA_KEYS = { StructuredName.GIVEN_NAME, Organization.COMPANY, Email.DATA, Phone.NUMBER };
		private static String[] TYPE_KEYS = { null, null, Email.TYPE, Phone.TYPE };
		private static Integer[] TYPES = { null, null, Email.TYPE_WORK, Phone.TYPE_WORK };
		private static String[][] EXTRA_KEYS = { { StructuredName.FAMILY_NAME }, { Organization.TITLE }, null, null };
		private static String[][] EXTRA_FIELDS = { { ISweetContact.LAST_NAME_KEY }, { ISweetContact.TITLE_KEY }, null,
				null };
	}

	/**
	 * Syncs a list of contacts obtained from the server. Uses batch operations
	 * for efficiency
	 * 
	 * @param contacts
	 *            A list of objects obtained from the Sugar Server implementing
	 *            the ISweetContact interface.
	 */

	public static int syncContacts(Context context, Account acc, List<ISweetContact> contacts) {
		Log.i(TAG, "syncContacts()");
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		getAccountType(context);
		ContentResolver resolver = context.getContentResolver();
		Log.i(TAG, "Starting to sync locally");
		int i = 0;
		for (ISweetContact c : contacts) {

			long localId = findLocalContact(resolver, c.getId());
			if (localId == 0) {
				addContact(resolver, ops, acc.name, c);
			} else {
				updateContact(resolver, ops, acc.name, c, localId);
			}
			i++;
			try {
				if (i % 100 == 0) {
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

		}
		try {
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
			return 0;
		}
		if (c.moveToFirst()) {
			long rawId = c.getLong(0);
			c.close();
			return rawId;// Return first column!
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
		// must be 1 and only 1!
		if (c.getCount() != 1) {
			return 0;
		}
		if (c.moveToFirst()) {
			long dataId = c.getLong(0);
			c.close();
			return dataId;// Return first column!
		}
		return 0;
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
		builder.withValues(values);
		ops.add(builder.build());
		addContactData(ops, contact, values, reference);
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
			String accountName, ISweetContact contact, long rawId) {
		ContentValues values = new ContentValues();
		updateContactData(resolver, ops, contact, values, rawId);
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
			ISweetContact contact, ContentValues values, long rawId) {
		// TODO: Fix the case where new data has been added (for instance email
		// before was not set and now it is)
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
				if ((data_key != null) && (data != null) && !TextUtils.isEmpty(data)) {
					values.put(mimetype_key, mimetype);
					values.put(data_key, data);
					if (type_key != null)
						values.put(type_key, type);
					if (extra_keys != null) {
						for (int j = 0; j < extra_keys.length; j++)
							values.put(extra_keys[j], contact.get(extra_fields[j]));
					}
				}
				long dataId = findContactField(resolver, rawId, field);
				if (dataId != 0) { // if the row exists, update it
					ContentProviderOperation.Builder builder = getDataUpdateBuilder(dataId);
					if (values.size() > 0) {
						builder.withValues(values);
						ops.add(builder.build());
					}
				} else {
					// field didn't exists previously, insert it
					ContentProviderOperation.Builder builder = getDataInsertBuilder();
					if ((data_key != null) && (data != null) && !TextUtils.isEmpty(data)) {
						if (values.size() > 0) {
							values.put(Data.RAW_CONTACT_ID, rawId);
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
	 * 
	 */
	private static ContentProviderOperation.Builder getRawContactInsertBuilder() {
		return ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(RawContacts.CONTENT_URI))
				.withYieldAllowed(true);
	}

	private static ContentProviderOperation.Builder getDataUpdateBuilder(long dataId) {
		return ContentProviderOperation.newUpdate(addCallerIsSyncAdapterParameter(ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, dataId)))
				.withYieldAllowed(false);
	}

	private static ContentProviderOperation.Builder getDataInsertBuilder() {
		return ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI))
				.withYieldAllowed(false);
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
