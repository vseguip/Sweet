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
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

public class SweetContactSync extends AbstractThreadedSyncAdapter {
	Context mContext;
	private String AUTH_TOKEN_TYPE;
	private AccountManager mAccountManager;
	private String mAuthToken;
	private final String TAG = "SweetContactSync";
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
		Log.i(TAG,"onPerformSync()");
		performNetOperation(new SugarRunnable(account, new ISugarRunnable() {
			@Override
			public void run() throws URISyntaxException, OperationCanceledException, AuthenticatorException,
					IOException, AuthenticationException {
				Log.i(TAG,"Running PerformSync closure()");
				String server = mAccountManager.getUserData(account, SweetAuthenticatorActivity.KEY_PARAM_SERVER);
				mAuthToken =  mAccountManager.blockingGetAuthToken(account, AUTH_TOKEN_TYPE	, true);
				
				SugarAPI sugar = SugarAPIFactory.getSugarAPI(server);
				List<ISweetContact> contacts = sugar.getNewerContacts(mAuthToken, null);
				for(ISweetContact c: contacts){
					Log.i(TAG, "Retreived contact " + c.getFirstName());
				}
			}
		}));
	}

	void performNetOperation(Runnable r) {
		// TODO: Run in background?
		r.run();
	}
}
