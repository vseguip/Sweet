/********************************************************************\

File: SugarAPIFactory.java
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

package com.github.vseguip.sweet.rest;

import java.net.URISyntaxException;

import android.accounts.Account;
import android.accounts.AccountManager;

import com.github.vseguip.sweet.SweetAuthenticatorActivity;
import com.github.vseguip.sweet.utils.Utils;

public class SugarAPIFactory {
	private static SugarRestAPI api;

	public static synchronized SugarAPI getSugarAPI(AccountManager am, Account account) throws URISyntaxException {
		final String server = am.getUserData(account, SweetAuthenticatorActivity.KEY_PARAM_SERVER);
		return getSugarAPI(am, account, server);
		
	}
	private static synchronized SugarAPI getSugarAPI(AccountManager am, Account account, String server) throws URISyntaxException {
		final boolean validation = Utils.getBooleanAccountData(am, account, SweetAuthenticatorActivity.KEY_PARAM_VALIDATE, true);
		final boolean encrypt = Utils.getBooleanAccountData(am, account, SweetAuthenticatorActivity.KEY_PARAM_ENCRYPT, true);
		
		return getSugarAPI(server, validation, encrypt);
		
	}
	
	public static synchronized SugarAPI getSugarAPI(String server, boolean noCertValidation, boolean encryptPasswd) throws URISyntaxException {
		if (api == null) {
			api = new SugarRestAPI(server, noCertValidation, encryptPasswd);
		} else {
			api.setServer(server, noCertValidation, encryptPasswd);
		}
		return api;
	}

}
