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

import android.text.TextUtils;

import com.github.vseguip.sweet.contacts.ISweetContact;

public class SweetContact implements ISweetContact {

	Map<String, String> mValues;

	public SweetContact(SweetContact c) {
		mValues = new HashMap<String, String>(c.mValues);
	}

	public ISweetContact deepCopy() {
		return new SweetContact(this);
	}

	public ISweetContact mergeContact(ISweetContact c) {
		SweetContact merged = new SweetContact(this);

		if (c != null) {
			for (String fieldId : mValues.keySet()) {
				String thisVal = merged.get(fieldId);
				String otherVal = c.get(fieldId);
				if (TextUtils.isEmpty(thisVal) && !TextUtils.isEmpty(otherVal)) {
					merged.set(fieldId, otherVal);
				}
			}
		}
		return merged;
	}

	public SweetContact() {
		super();
		mValues = new HashMap<String, String>();
		mValues.put(ID_KEY, "");
		mValues.put(FIRST_NAME_KEY, "");
		mValues.put(LAST_NAME_KEY, "");
		mValues.put(TITLE_KEY, "");
		mValues.put(ACCOUNT_NAME_KEY, "");
		mValues.put(ACCOUNT_ID_KEY, "");
		mValues.put(EMAIL1_KEY, "");
		mValues.put(WORK_PHONE_KEY, "");
		mValues.put(MOBILE_PHONE_KEY, "");
		mValues.put(WORK_FAX_KEY, "");
		mValues.put(STREET_KEY, "");
		mValues.put(CITY_KEY, "");
		mValues.put(STATE_KEY, "");
		mValues.put(POSTAL_CODE_KEY, "");
		mValues.put(COUNTRY_KEY, "");
		setDateModified(null);
	}

	public SweetContact(String id, String firstName, String lastName, String title, String accountName,
			String accountId, String email1, String phoneWork, String mobilePhone, String workFax, String street,
			String city, String state, String postalCode, String country, String date) {
		super();
		mValues = new HashMap<String, String>();
		mValues.put(ID_KEY, id);
		mValues.put(FIRST_NAME_KEY, firstName);
		mValues.put(LAST_NAME_KEY, lastName);
		mValues.put(TITLE_KEY, title);
		mValues.put(ACCOUNT_NAME_KEY, accountName);
		mValues.put(ACCOUNT_ID_KEY, accountId);
		mValues.put(EMAIL1_KEY, email1);
		mValues.put(WORK_PHONE_KEY, phoneWork);
		mValues.put(MOBILE_PHONE_KEY, mobilePhone);
		mValues.put(WORK_FAX_KEY, workFax);
		mValues.put(STREET_KEY, street);
		mValues.put(CITY_KEY, city);
		mValues.put(STATE_KEY, state);
		mValues.put(POSTAL_CODE_KEY, postalCode);
		mValues.put(COUNTRY_KEY, country);
		setDateModified(date);

	}

	@Override
	public String getId() {
		return mValues.get(ID_KEY);
	}

	@Override
	public void setId(String id) {
		mValues.put(ID_KEY, id);
	}

	@Override
	public String getFirstName() {
		return mValues.get(FIRST_NAME_KEY);
	}

	@Override
	public void setFirstName(String firstName) {
		mValues.put(FIRST_NAME_KEY, firstName);
	}

	@Override
	public String getLastName() {
		return mValues.get(LAST_NAME_KEY);
	}

	@Override
	public void setLastName(String lastName) {
		mValues.put(LAST_NAME_KEY, lastName);
	}

	@Override
	public String getTitle() {
		return mValues.get(TITLE_KEY);
	}

	@Override
	public void setTitle(String title) {
		mValues.put(TITLE_KEY, title);
	}

	@Override
	public String getAccountName() {
		return mValues.get(ACCOUNT_NAME_KEY);
	}

	@Override
	public void setAccountName(String accountName) {
		mValues.put(ACCOUNT_NAME_KEY, accountName);
	}

	@Override
	public String getAccountId() {
		return mValues.get(ACCOUNT_ID_KEY);
	}

	@Override
	public void setAccountId(String accountId) {
		mValues.put(ACCOUNT_ID_KEY, accountId);
	}

	@Override
	public String getEmail1() {
		return mValues.get(EMAIL1_KEY);
	}

	@Override
	public void setEmail1(String email1) {
		mValues.put(EMAIL1_KEY, email1);
	}

	@Override
	public String getWorkPhone() {
		return mValues.get(WORK_PHONE_KEY);
	}

	@Override
	public void setWorkPhone(String phoneWork) {
		mValues.put(WORK_PHONE_KEY, phoneWork);
	}

	@Override
	public String getMobilePhone() {
		return mValues.get(MOBILE_PHONE_KEY);
	}

	@Override
	public void setMobilePhone(String mobilePhone) {
		mValues.put(MOBILE_PHONE_KEY, mobilePhone);
	}

	@Override
	public String getWorkFax() {
		return mValues.get(WORK_FAX_KEY);
	}

	@Override
	public void setWorkFax(String faxWork) {
		mValues.put(WORK_FAX_KEY, faxWork);
	}

	@Override
	public String getDateModified() {
		return mValues.get(DATE_MODIFIED_KEY);
	}

	@Override
	public void setDateModified(String date) {
		mValues.put(DATE_MODIFIED_KEY, date);
	}

	@Override
	public void set(String field, String data) {
		mValues.put(field, data);
	}

	@Override
	public String get(String field) {
		return mValues.get(field);
	}

	@Override
	public String getCity() {
		return mValues.get(CITY_KEY);
	}

	@Override
	public String getCountry() {
		return mValues.get(COUNTRY_KEY);
	}

	@Override
	public String getPostalCode() {
		return mValues.get(POSTAL_CODE_KEY);
	}

	@Override
	public String getRegion() {
		return mValues.get(STATE_KEY);
	}

	@Override
	public String getStreet() {
		return mValues.get(STREET_KEY);
	}

	@Override
	public void setCity(String city) { // 
		mValues.put(CITY_KEY, city);

	}

	@Override
	public void setCountry(String country) {
		mValues.put(COUNTRY_KEY, country);

	}

	@Override
	public void setPostalCode(String postalCode) {
		mValues.put(POSTAL_CODE_KEY, postalCode);

	}

	@Override
	public void setRegion(String region) {
		mValues.put(STATE_KEY, region);

	}

	@Override
	public void setStreet(String street) {
		mValues.put(STREET_KEY, street);

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mValues == null) ? 0 : mValues.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SweetContact other = (SweetContact) obj;
		if (mValues == null) {
			if (other.mValues != null)
				return false;
		} else if (!mValues.equals(other.mValues))
			return false;
		return true;
	}

	@Override
	public String getDisplayName() {
		return getFirstName() + " " + getLastName();
	}

	@Override
	public boolean equalUIFields(ISweetContact c) {
		for (String field : ISweetContact.COMPARISON_FIELDS) {
			if (!this.get(field).equals(c.get(field)))
				return false;
		}
		return true;
	}
}
