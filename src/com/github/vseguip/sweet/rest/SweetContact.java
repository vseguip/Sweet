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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.github.vseguip.sweet.contacts.ISweetContact;

public class SweetContact implements ISweetContact {

	Map<String, String> mValues;
	
	public SweetContact(String id, String firstName, String lastName, String title, String accountName,
			String accountId, String email1, String phoneWork, String date) {
		super();
		mValues = new HashMap<String, String>();
		mValues.put(ID_KEY, id);
		mValues.put(FIRST_NAME_KEY, firstName);
		mValues.put(LAST_NAME_KEY, lastName);
		mValues.put(TITLE_KEY, title);
		mValues.put(ACCOUNT_NAME_KEY, accountName);
		mValues.put(ACCOUNT_ID_KEY, accountId);
		mValues.put(EMAIL1_KEY, email1);
		mValues.put(PHONE_WORK_KEY, phoneWork);
		setDateModified(date);
	}
	
	public String getId() {
		return mValues.get(ID_KEY);
	}

	public void setId(String id) {
		mValues.put(ID_KEY, id);		
	}
	
	public String getFirstName() {
		return mValues.get(FIRST_NAME_KEY);
	}

	public void setFirstName(String firstName) {
		mValues.put(FIRST_NAME_KEY, firstName);		
	}
	public String getLastName() {
		return mValues.get(LAST_NAME_KEY);
	}
	public void setLastName(String lastName) {
		mValues.put(LAST_NAME_KEY, lastName);		
	}
	public String getTitle() {
		return mValues.get(TITLE_KEY);
	}
	public void setTitle(String title) {
		mValues.put(TITLE_KEY, title);
	}
	public String getAccountName() {
		return mValues.get(ACCOUNT_NAME_KEY);
	}
	public void setAccountName(String accountName) {
		mValues.put(ACCOUNT_NAME_KEY, accountName);
	}
	public String getAccountId() {
		return mValues.get(ACCOUNT_ID_KEY);
	}
	public void setAccountId(String accountId) {
		mValues.put(ACCOUNT_ID_KEY, accountId);
	}
	public String getEmail1() {
		return mValues.get(EMAIL1_KEY);
	}
	public void setEmail1(String email1) {
		mValues.put(EMAIL1_KEY, email1);
	}
	public String getPhoneWork() {
		return mValues.get(PHONE_WORK_KEY);
	}
	public void setPhoneWork(String phoneWork) {
		mValues.put(PHONE_WORK_KEY, phoneWork);
	}

	public String getDateModified() {
		return mValues.get(DATE_MODIFIED_KEY);
	}
	public void setDateModified(String date) {
		mValues.put(DATE_MODIFIED_KEY, date);
	}
	
	public void set(String field, String data) {
		mValues.put(field, data);		
	}
	public String get(String field) {
		return mValues.get(field);
	}
}
