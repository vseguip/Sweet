package com.github.vseguip.sweet;

import java.net.URISyntaxException;

import com.github.vseguip.sweet.rest.SugarAPI;
import com.github.vseguip.sweet.rest.SugarAPIFactory;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/********************************************************************
 * \
 * 
 * File: SweetAuthenticator.java Copyright 2011 Vicent Seguí Pascual
 * 
 * This file is part of Sweet. Sweet is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * Sweet is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * Sweet.
 * 
 * If not, see http://www.gnu.org/licenses/. \
 ********************************************************************/

public class SweetAuthenticator extends AbstractAccountAuthenticator {
	private final String TAG = "SweetAuthenticator";
	private final Context mContext;

	public SweetAuthenticator(Context _context) {
		super(_context);
		mContext = _context;
	}

	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType,
			String[] requiredFeatures, Bundle options) throws NetworkErrorException {
		// Adapted from SampleSyncAdapter
		Log.i(TAG, "addAccount()");
		final Intent intent = new Intent(mContext, SweetAuthenticatorActivity.class);
		intent.putExtra(SweetAuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
		intent.putExtra(SweetAuthenticatorActivity.PARAM_CREATE_ACCOUNT, true);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

		final Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		return bundle;

	}

	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options)
			throws NetworkErrorException {
		// if we have the appropriate information, try to validate
		Log.i(TAG, "confirmCredentials()");
		if (options != null && options.containsKey(AccountManager.KEY_PASSWORD)
				&& options.containsKey(AccountManager.KEY_USERDATA)) {
			final String password = options.getString(AccountManager.KEY_PASSWORD);
			AccountManager am = AccountManager.get(mContext);
			final String server = am.getUserData(account, SweetAuthenticatorActivity.KEY_PARAM_SERVER);
			final boolean verified = validateUser(account.name, password, server);
			final Bundle result = new Bundle();
			result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, verified);
			return result;
		}
		// Launch AuthenticatorActivity to confirm credentials
		final Intent intent = new Intent(mContext, SweetAuthenticatorActivity.class);
		intent.putExtra(SweetAuthenticatorActivity.PARAM_USERNAME, account.name);
		intent.putExtra(SweetAuthenticatorActivity.PARAM_CONFIRM_CREDENTIALS, true);
		intent.putExtra(SweetAuthenticatorActivity.PARAM_CREATE_ACCOUNT, false);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		final Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		return bundle;
	}

	private boolean validateUser(String username, String passwd, String server) {
		Log.i(TAG, "validateUser()");
		return getServerAuthToken(username, passwd, server) == null;
	}

	private String getServerAuthToken(String username, String passwd, String server) {
		Log.i(TAG, "onlineConfirmPassword()");
		SugarAPI sugar;
		String authToken = null;
		try {
			sugar = SugarAPIFactory.getSugarAPI(server);
			authToken = sugar.getToken(username, passwd, null, null);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			Log.e(TAG, "Error URI is invalid " + server);
		}
		return authToken;
	}

	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
		Log.i(TAG, "editProperties");
		return null;
	}

	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType,
			Bundle options) throws NetworkErrorException {
		Log.i(TAG, "getAuthToken()");
		final String AUTHTOKEN_TYPE = mContext.getString(R.string.account_type);
		if (!authTokenType.equals(AUTHTOKEN_TYPE)) {
			final Bundle result = new Bundle();
			result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
			return result;
		}
		final AccountManager am = AccountManager.get(mContext);
		final String password = am.getPassword(account);
		final String server = am.getUserData(account, SweetAuthenticatorActivity.KEY_PARAM_SERVER);
		if ((password != null) || (server != null)) {
			final String authToken = getServerAuthToken(account.name, password, server);
			if (authToken != null) {
				final Bundle result = new Bundle();
				result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
				result.putString(AccountManager.KEY_ACCOUNT_TYPE, mContext.getString(R.string.account_type));
				result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
				return result;
			}
		}
		// the password was missing or incorrect, return an Intent to an
		// Activity that will prompt the user for the password.
		final Intent intent = new Intent(mContext, SweetAuthenticatorActivity.class);
		intent.putExtra(SweetAuthenticatorActivity.PARAM_USERNAME, account.name);
		intent.putExtra(SweetAuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		final Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		return bundle;
	}

	@Override
	public String getAuthTokenLabel(String authTokenType) {
		Log.i(TAG, "getAuthTokenLabel()");
		if (mContext.getString(R.string.account_type).equals(authTokenType)) {
			return mContext.getString(R.string.account_label);
		}
		return null;

	}

	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features)
			throws NetworkErrorException {
		Log.i(TAG, "hasFeatures()");
		final Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
		return result;
	}

	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType,
			Bundle options) throws NetworkErrorException {
		Log.i(TAG, "updateCredentials()");

		final Intent intent = new Intent(mContext, SweetAuthenticatorActivity.class);
		intent.putExtra(SweetAuthenticatorActivity.PARAM_USERNAME, account.name);
		intent.putExtra(SweetAuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
		intent.putExtra(SweetAuthenticatorActivity.PARAM_CONFIRM_CREDENTIALS, false);
		intent.putExtra(SweetAuthenticatorActivity.PARAM_CREATE_ACCOUNT, false);
		final Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		return bundle;
	}

}
