/********************************************************************\

File: SweetMainActivity.java
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

package com.github.vseguip.sweet;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.os.Bundle;

public class SweetMainActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AccountManager am = (AccountManager) getSystemService(ACCOUNT_SERVICE);
		AccountManagerCallback<Bundle> completeCallback = new AccountManagerCallback<Bundle>() {
			@Override
			public void run(AccountManagerFuture<Bundle> arg0) {
				SweetMainActivity.this.finish();
			}
		};
		String authTokenType = getString(R.string.account_type);
		Account[] accounts = am.getAccountsByType(getString(R.string.account_type));
		if ((accounts != null) && (accounts.length > 0)) {
			am.updateCredentials(accounts[0], authTokenType, null, this, completeCallback, null);
		} else {
			am.addAccount(authTokenType, authTokenType, null, null, this, completeCallback, null);
		}
		finish();
	}
}
