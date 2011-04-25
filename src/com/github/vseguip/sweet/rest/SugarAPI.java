/********************************************************************\

File: SugarAPI.java
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.auth.AuthenticationException;

import com.github.vseguip.sweet.contacts.ISweetContact;

import android.content.Context;
import android.os.Handler;

public interface SugarAPI {
	public abstract void setServer(String server) throws URISyntaxException;

	/**
	 * Get contacts created, modified or deleted since a date.
	 * 
	 * @param token
	 *            The SessionID token gotten by getToken
	 * @param date
	 *            The date we want to use as a comparison. If null we will fetch
	 *            all contacts, if not we will fetch contacts with modified_date
	 *            greater than or equal to (>=) date.
	 * 
	 */
	public List<ISweetContact> getNewerContacts(String token, String date) throws AuthenticationException, IOException;

	public abstract String getToken(String username, String passwd, Context context, Handler handler);

}