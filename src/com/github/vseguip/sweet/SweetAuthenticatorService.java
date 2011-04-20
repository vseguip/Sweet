/********************************************************************\


File: SweetAuthenticatorService.java
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

/**** Service that creates the authenticator object ****/
import android.accounts.AbstractAccountAuthenticator;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class SweetAuthenticatorService extends Service {
	private static final String TAG = "SweethAuthenticatorService";
	private AbstractAccountAuthenticator m_accountAuthenticator;

	@Override
	public IBinder onBind(Intent intent) {
		if (intent.getAction().equals(AccountManager.ACTION_AUTHENTICATOR_INTENT)) {

			return getAuthenticator().getIBinder();
		}
		Log.i(TAG, "Binding authenticator with intent " + intent.getAction());
		return getAuthenticator().getIBinder();
	}

	private synchronized AbstractAccountAuthenticator getAuthenticator() {

		if (m_accountAuthenticator == null)
			m_accountAuthenticator = new SweetAuthenticator(this);
		return m_accountAuthenticator;

	}

}
