/********************************************************************\

File: SweetContact.java
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

import java.util.HashMap;
import java.util.Map;

import com.github.vseguip.sweet.contacts.ISweetContact;

public class SweetContact implements ISweetContact {

	Map<String, String> values;
	public SweetContact(String id, String firstName, String lastName, String title, String accountName,
			String accountId, String email1, String phoneWork) {
		super();
		values = new HashMap<String, String>();
		values.put(ID_KEY, id);
		values.put(FIRST_NAME_KEY, firstName);
		values.put(LAST_NAME_KEY, lastName);
		values.put(TITLE_KEY, title);
		values.put(ACCOUNT_NAME_KEY, accountName);
		values.put(ACCOUNT_ID_KEY, accountId);
		values.put(EMAIL1_KEY, email1);
		values.put(PHONE_WORK_KEY, phoneWork);
		values.put(DISPLAY_NAME_KEY, firstName +  " " + lastName);
	}
	
	public String getId() {
		return values.get(ID_KEY);
	}

	public void setId(String id) {
		values.put(ID_KEY, id);		
	}

	public String getDisplayName() {
		return values.get(DISPLAY_NAME_KEY);
	}
	public String getFirstName() {
		return values.get(FIRST_NAME_KEY);
	}

	public void setFirstName(String firstName) {
		values.put(FIRST_NAME_KEY, firstName);
		values.put(DISPLAY_NAME_KEY, firstName +  " " + getLastName());
	}
	public String getLastName() {
		return values.get(LAST_NAME_KEY);
	}
	public void setLastName(String lastName) {
		values.put(LAST_NAME_KEY, lastName);
		values.put(DISPLAY_NAME_KEY, getFirstName() +  " " + lastName);
	}
	public String getTitle() {
		return values.get(TITLE_KEY);
	}
	public void setTitle(String title) {
		values.put(TITLE_KEY, title);
	}
	public String getAccountName() {
		return values.get(ACCOUNT_NAME_KEY);
	}
	public void setAccountName(String accountName) {
		values.put(ACCOUNT_NAME_KEY, accountName);
	}
	public String getAccountId() {
		return values.get(ACCOUNT_ID_KEY);
	}
	public void setAccountId(String accountId) {
		values.put(ACCOUNT_ID_KEY, accountId);
	}
	public String getEmail1() {
		return values.get(EMAIL1_KEY);
	}
	public void setEmail1(String email1) {
		values.put(EMAIL1_KEY, email1);
	}
	public String getPhoneWork() {
		return values.get(PHONE_WORK_KEY);
	}
	public void setPhoneWork(String phoneWork) {
		values.put(PHONE_WORK_KEY, phoneWork);
	}
	public void set(String field, String data) {
		values.put(field, data);
		values.put(DISPLAY_NAME_KEY, getFirstName() +  " " + getLastName());
	}
	public String get(String field) {
		return values.get(field);
	}
}
