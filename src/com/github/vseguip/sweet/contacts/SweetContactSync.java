/********************************************************************\

File: SweetContactSync.java

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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.auth.AuthenticationException;

import com.github.vseguip.sweet.R;
import com.github.vseguip.sweet.SweetAuthenticatorActivity;
import com.github.vseguip.sweet.rest.SugarAPI;
import com.github.vseguip.sweet.rest.SugarAPIFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * SweetContactSync
 * 
 * @author vseguip
 * 
 * 
 *         SugarCRM return date_modified field as a String (in GMT) which can be
 *         lexicographically compared to determine which date is the latest. The
 *         sync strategy is as follows
 * 
 *         1) If the account "lastSync" user data is null perform a full sync
 * 
 *         2) If the account "lastSync" field contains a String use it to get
 *         only newer contacts.Everytime we get new contacts we search for the
 *         latest modified time and we get all contacts with modification time
 *         greater than or equal to the last sync time. This is done since we
 *         can have a newer contact inserted in SugarCRM with the same date
 *         since SugarCRM only keeps second accuracy. This means we will
 *         probably get the latest entry also but that's not really a problem.
 * 
 *         3) Store last modified date into the account user data for future
 *         use.
 * 
 *         NOTE: 3 things about this strategy a) All times used are referenced
 *         to SugarCRM time system which means there are no conflicts between
 *         differing time zones in the server or client. No special care has to
 *         be taken
 * 
 *         b) We never don't need to parse the time in the client so again no
 *         problems when it comes to different time zones, etc
 * 
 *         c) The user can force a full sync using the account preferences to
 *         set the lastSync user data to null.
 * 
 *         Contacts conlict resolution:
 * 
 *         1. Get newer contacts from SugarCRM
 * 
 *         2. Get dirty local modified contacts
 * 
 *         3. Get locally created contacts
 * 
 *         4. Set of conflicts id found by performing intersection on SourceID
 *         between 1 and 2
 * 
 *         5. Update non conflicting data to local
 * 
 *         6. Send new contacts to remote and update the local source ID's
 * 
 *         7. Send non conflicting data to remote
 * 
 *         8. for each conflicting contact in Set of conflicting contacts do One
 *         contact resolution
 * 
 * 
 * 
 *         One contact resolution
 * 
 *         1. Find set of differing fields
 * 
 *         2. If field exists locally but not remotely upload to sugar
 * 
 *         3. If field exists remotely but not locally update locally
 * 
 *         4. If field different in both server and locally use preference to
 *         decide which one to keep
 * 
 */
public class SweetContactSync extends AbstractThreadedSyncAdapter {

	Context mContext;
	private String AUTH_TOKEN_TYPE;
	private AccountManager mAccountManager;
	private String mAuthToken;
	private final String TAG = "SweetContactSync";
	static final String LAST_SYNC_KEY = "lastSync";

	public SweetContactSync(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		Log.i(TAG, "SweetContactSync");
		mContext = context;
		AUTH_TOKEN_TYPE = mContext.getString(R.string.account_type);
		mAccountManager = AccountManager.get(mContext);
	}

	public interface ISugarRunnable {
		public void run() throws URISyntaxException, OperationCanceledException, AuthenticatorException, IOException,
				AuthenticationException;
	}

	class SugarRunnable implements Runnable {
		ISugarRunnable r;
		Account mAccount;
		SyncResult mSyncResult;

		public SugarRunnable(Account acc, SyncResult syncResult, ISugarRunnable _r) {
			r = _r;
			mAccount = acc;
			mSyncResult = syncResult;
		}

		@Override
		public void run() {
			try {
				r.run();
			} catch (URISyntaxException ex) {
				if (mAccount != null)
					mAccountManager.confirmCredentials(mAccount, null, null, null, null);
			} catch (OperationCanceledException e) {
				mSyncResult.stats.numConflictDetectedExceptions++;
				e.printStackTrace();
			} catch (AuthenticatorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (AuthenticationException aex) {
				// if (SweetContactSync.this.mAuthToken != null) {
				mAccountManager.invalidateAuthToken(AUTH_TOKEN_TYPE, SweetContactSync.this.mAuthToken);
				// } else {
				//					
				// mAccountManager.confirmCredentials(mAccount, null, null,
				// null, null);
				// }
				mSyncResult.stats.numAuthExceptions++;
			}

		}

	}

	@Override
	public void onPerformSync(final Account account, Bundle extras, String authority, ContentProviderClient provider,
			SyncResult syncResult) {
		Log.i(TAG, "onPerformSync()");
		// Get preferences
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		boolean fullSync = settings.getBoolean(mContext.getString(R.string.full_sync), false);
		if (fullSync)
			mAccountManager.setUserData(account, LAST_SYNC_KEY, null);
		performNetOperation(new SugarRunnable(account, syncResult, new ISugarRunnable() {
			@Override
			public void run() throws URISyntaxException, OperationCanceledException, AuthenticatorException,
					IOException, AuthenticationException {
				Log.i(TAG, "Running PerformSync closure()");
				String server = mAccountManager.getUserData(account, SweetAuthenticatorActivity.KEY_PARAM_SERVER);
				try {
				mAuthToken = mAccountManager.blockingGetAuthToken(account, AUTH_TOKEN_TYPE, true);
				} catch (Exception ex){
					ex.printStackTrace();
				}
				//try again, it could be due to an invalid session
				if(mAuthToken==null){
					mAuthToken = mAccountManager.blockingGetAuthToken(account, AUTH_TOKEN_TYPE, true);
				}
				SugarAPI sugar = SugarAPIFactory.getSugarAPI(server);

				String lastDate = mAccountManager.getUserData(account, LAST_SYNC_KEY);
				List<ISweetContact> contacts = sugar.getNewerContacts(mAuthToken, lastDate);
				List<ISweetContact> modifiedContacts = ContactManager.getLocallyModifiedContacts(mContext, account);
				List<ISweetContact> createdContacts = ContactManager.getLocallyCreatedContacts(mContext, account);
				// Get latest date from server
				for (ISweetContact c : contacts) {
					String contactDate = c.getDateModified();
					if ((lastDate == null) || (lastDate.compareTo(contactDate) < 0)) {
						lastDate = contactDate;
					}
				}
				// Determine conflicting contacts
				Set<String> conflictSet = getConflictSet(contacts, modifiedContacts);
				Map<String, ISweetContact> conflictingSugarContacts = filterIds(contacts, conflictSet);
				Map<String, ISweetContact> conflictingLocalContacts = filterIds(modifiedContacts, conflictSet);

				if (modifiedContacts.size() > 0) {
					// Send modified local non conflicting contacts to the
					// server
					List<String> newIds = sugar.sendNewContacts(mAuthToken, modifiedContacts, false);
					if (newIds.size() != modifiedContacts.size()) {
						throw new OperationCanceledException("Error updating local contacts in the remote server");
					}
					ContactManager.cleanDirtyFlag(mContext, modifiedContacts);
				}
				if (createdContacts.size() > 0) {
					List<String> newIds = sugar.sendNewContacts(mAuthToken, createdContacts, true);
					if (newIds.size() != createdContacts.size()) {
						// something wrong happened, it's probable the user will
						// have to clear the data
						throw new OperationCanceledException("Error creating local contacts in the remote server");
					}
					ContactManager.assignSourceIds(mContext, createdContacts, newIds);
					ContactManager.cleanDirtyFlag(mContext, createdContacts);
				}
				// Sync remote contacts locally.
				if (contacts.size() > 0) {
					ContactManager.syncContacts(mContext, account, contacts);
				}
				// resolve remaining conflicts
				List<ISweetContact> resolvedContacts = new ArrayList<ISweetContact>();
				for (String id : conflictSet) {
					ISweetContact local = conflictingLocalContacts.get(id);
					ISweetContact remote = conflictingSugarContacts.get(id);
					if (local.equals(remote)) {
						// no need to sync
						resolvedContacts.add(local);
						conflictingLocalContacts.remove(id);
						conflictingSugarContacts.remove(id);
					} else {
						Log.i(TAG, "Local contact differs from remote contact " + local.getFirstName() + " "
								+ local.getLastName());
					}

				}
				ContactManager.cleanDirtyFlag(mContext, resolvedContacts);
				if (conflictingLocalContacts.size() > 0) {
					//Create a notification that can launch an mActivity to resolve the pending conflict
					NotificationManager nm = (NotificationManager) mContext
							.getSystemService(Context.NOTIFICATION_SERVICE);
					Notification notify = new Notification(R.drawable.icon, mContext.getString(R.string.notify_sync_conflict_ticket), System
							.currentTimeMillis());
					Intent intent = new Intent(mContext, SweetConflictResolveActivity.class);
					intent.putExtra("account", account);
					SweetConflictResolveActivity.storeConflicts(conflictingLocalContacts, conflictingSugarContacts);
					
					notify.setLatestEventInfo(
												mContext,
												mContext.getString(R.string.notify_sync_conflict_title),
												mContext.getString(R.string.notify_sync_conflict_message),
												PendingIntent.getActivity(
																			mContext,
																			0,
																			intent,
																			PendingIntent.FLAG_CANCEL_CURRENT));
					nm.notify(SweetConflictResolveActivity.NOTIFY_CONFLICT, SweetConflictResolveActivity.NOTIFY_CONTACT, notify);
					throw new OperationCanceledException("Pending conflicts");
				}
				// Save the last sync time in the account if all went ok
				mAccountManager.setUserData(account, LAST_SYNC_KEY, lastDate);
			}
		}));
	}

	Map<String, ISweetContact> filterIds(List<ISweetContact> contacts, Set<String> filter) {
		Map<String, ISweetContact> filteredContacts = new HashMap<String, ISweetContact>();
		Iterator<ISweetContact> it = contacts.iterator();
		while (it.hasNext()) {
			ISweetContact contact = it.next();
			if (filter.contains(contact.getId())) {
				it.remove();
				filteredContacts.put(contact.getId(), contact);
			}
		}
		return filteredContacts;
	}

	Set<String> getConflictSet(List<ISweetContact> list1, List<ISweetContact> list2) {
		Set<String> set1 = getSetOfIds(list1);
		Set<String> set2 = getSetOfIds(list2);
		set1.retainAll(set2);
		return set1;
	}

	Set<String> getSetOfIds(List<ISweetContact> contacts) {
		Set<String> ids = new HashSet<String>();
		if (contacts != null) {
			for (ISweetContact c : contacts) {
				ids.add(c.getId());
			}
		}
		return ids;
	}

	void performNetOperation(Runnable r) {
		r.run();
	}
}
