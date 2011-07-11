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

	/**
	 * Set the remote server URI.
	 * 
	 * @param server
	 *            The URI to the remote JSON rest.php (e.g.
	 *            http://localhost/sugarcrm/service/v2/rest.php
	 * @throws URISyntaxException
	 * 
	 */

	public abstract void setServer(String server, boolean useSSL, boolean clearPassword) throws URISyntaxException;

	/**
	 * Login the SugarCRM and get an autorization token to be used in future
	 * calls
	 * 
	 * @param username
	 *            The SugarCRM username
	 * @param passwd
	 *            The sugarCRM password
	 * @param context
	 *            The context invoking the call
	 * @param handler
	 *            A handler object to post the results asynchronously
	 * @return The authorization token
	 * 
	 */

	public abstract String getToken(String username, String passwd, Context context, Handler handler);

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
	public abstract List<ISweetContact> getNewerContacts(String token, String date) throws AuthenticationException,
			IOException;

	/**
	 * Get contacts created, modified or deleted since a date, allows for "paging".
	 * 
	 * @param token
	 *            The SessionID token gotten by getToken
	 * @param date
	 *            The date we want to use as a comparison. If null we will fetch
	 *            all contacts, if not we will fetch contacts with modified_date
	 *            greater than or equal to (>=) date.
	 * @param start 
	 *            Retrieve from this contact on           
	 * @param count           
	 * 			  Retrieve this number of contacts
	 */
	public abstract List<ISweetContact> getNewerContacts(String token, String date, int start, int count) throws AuthenticationException,
			IOException;

	
	/**
	 * Send contacts to server and create them if needed. If contacts have no ID
	 * (e.g. they have been created locally but not in the server, get their new
	 * id and set it in the list.
	 * 
	 * @param token
	 *            The SessionID token gotten by getToken
	 * @param contact
	 *            The list of contacts we want to send/create
	 * 
	 * @return The List of ID's (in the same order) that was sent to the server.
	 *         If an object was created remotely this will contain the new Id
	 */
	public abstract List<String> sendNewContacts(String mAuthToken, List<ISweetContact> contacts, boolean createRemote) throws AuthenticationException,
	IOException;

}