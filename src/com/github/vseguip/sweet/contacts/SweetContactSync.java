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
import java.util.List;

import org.apache.http.auth.AuthenticationException;

import com.github.vseguip.sweet.R;
import com.github.vseguip.sweet.SweetAuthenticatorActivity;
import com.github.vseguip.sweet.rest.SugarAPI;
import com.github.vseguip.sweet.rest.SugarAPIFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class SweetContactSync extends AbstractThreadedSyncAdapter {
	
	Context mContext;
	private String AUTH_TOKEN_TYPE;
	private AccountManager mAccountManager;
	private String mAuthToken;
	private final String TAG = "SweetContactSync";
	private static final String LAST_SYNC_KEY = "lastSync";
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

		public SugarRunnable(Account acc, ISugarRunnable _r) {
			r = _r;
			mAccount = acc;
		}

		@Override
		public void run() {
			try {
				r.run();
			} catch (URISyntaxException ex) {
				if (mAccount != null)
					mAccountManager.confirmCredentials(mAccount, null, null, null, null);
			} catch (OperationCanceledException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (AuthenticatorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (AuthenticationException aex) {
				if (SweetContactSync.this.mAuthToken != null) {
					mAccountManager.invalidateAuthToken(AUTH_TOKEN_TYPE, SweetContactSync.this.mAuthToken);
				} else {
					mAccountManager.confirmCredentials(mAccount, null, null, null, null);
				}
			}

		}

	}

	@Override
	public void onPerformSync(final Account account, Bundle extras, String authority, ContentProviderClient provider,
			SyncResult syncResult) {
		Log.i(TAG, "onPerformSync()");
		//Get preferences
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		boolean fullSync = settings.getBoolean(mContext.getString(R.string.full_sync), false);
		if(fullSync)
			mAccountManager.setUserData(account, LAST_SYNC_KEY, null);
		performNetOperation(new SugarRunnable(account, new ISugarRunnable() {
			@Override
			public void run() throws URISyntaxException, OperationCanceledException, AuthenticatorException,
					IOException, AuthenticationException {
				Log.i(TAG, "Running PerformSync closure()");
				String server = mAccountManager.getUserData(account, SweetAuthenticatorActivity.KEY_PARAM_SERVER);
				mAuthToken = mAccountManager.blockingGetAuthToken(account, AUTH_TOKEN_TYPE, true);

				SugarAPI sugar = SugarAPIFactory.getSugarAPI(server);
				// SugarCRM return date_modified field as a String (in GMT)
				// which can be lexicographically compared to determine 
				// which date is the latest. The sync strategy is as follows
				// 1) If the account "lastSync" user data is null perform a full
				// sync
				// 2) If the account "lastSync" field contains a String use it
				// to get only newer contacts.Everytime we get new contacts we
				// search for the latest modified time and we get all contacts
				// with modification time greater than or equal to the last sync
				// time. This is done since we can have a newer contact 
				// inserted in SugarCRM with the same date since SugarCRM only 
				// keeps second accuracy. This means we will probably get the 
				// latest entry also but that's not really a problem.
				// 3) Store last modified date into the account user data for
				// future use.
				// 
				// NOTE: 3 things about this strategy
				// a) All times used are referenced to SugarCRM time system
				// which means there are no conflicts between differing time
				// zones in the server or client. No special care has to be
				// taken
				// b) We never don't need to parse the time in the client so
				// again no problems when it comes to different time zones, etc
				// c) The user can force a full sync using the account preferences 
				// to set the lastSync user data to null.
				String lastDate = mAccountManager.getUserData(account, LAST_SYNC_KEY);
				List<ISweetContact> contacts = sugar.getNewerContacts(mAuthToken, lastDate);
				for (ISweetContact c : contacts) {
					String contactDate = c.getDateModified();
					if ((lastDate == null) || (lastDate.compareTo(contactDate) < 0)) {
						lastDate = contactDate;
					}
				}
				ContactManager.syncContacts(mContext, account, contacts);
				// Save the last sync in the account
				mAccountManager.setUserData(account, LAST_SYNC_KEY, lastDate);
			}
		}));
	}

	void performNetOperation(Runnable r) {
		// TODO: Run in background?
		r.run();
	}
}
