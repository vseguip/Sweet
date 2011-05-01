/********************************************************************\

File: ISweetContact.java
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

public interface ISweetContact {
	public static final String ID_KEY = "id";
	public static final String MOBILE_PHONE_KEY = "mobilePhone";
	public static final String WORK_PHONE_KEY = "workPhone";
	public static final String WORK_FAX_KEY = "workFax";
	public static final String EMAIL1_KEY = "email1";
	public static final String ACCOUNT_ID_KEY = "accountId";
	public static final String ACCOUNT_NAME_KEY = "accountName";
	public static final String TITLE_KEY = "title";
	public static final String LAST_NAME_KEY = "lastName";
	public static final String FIRST_NAME_KEY = "firstName";

	public static final String STREET_KEY = "street";
	public static final String CITY_KEY = "city";
	public static final String STATE_KEY = "state";
	public static final String COUNTRY_KEY = "country";
	public static final String POSTAL_CODE_KEY = "postalCode";
	public static final String COMPARISON_FIELDS[] = { FIRST_NAME_KEY, LAST_NAME_KEY, ACCOUNT_NAME_KEY, TITLE_KEY,
			WORK_PHONE_KEY, MOBILE_PHONE_KEY, WORK_FAX_KEY, EMAIL1_KEY, STREET_KEY, CITY_KEY, POSTAL_CODE_KEY,
			STATE_KEY, COUNTRY_KEY };
	public static final String DATE_MODIFIED_KEY = "dateModified";

	public String getId();

	public void setId(String id);

	public String getFirstName();

	public void setFirstName(String firstName);

	public String getLastName();

	public void setLastName(String lastName);

	public String getAccountId();

	public void setAccountId(String accountId);

	public String getAccountName();

	public void setAccountName(String accountName);

	public String getWorkPhone();

	public void setWorkPhone(String phoneWork);

	public String getMobilePhone();

	public void setMobilePhone(String mobilePhone);

	public String getWorkFax();

	public void setWorkFax(String workFax);

	public String getEmail1();

	public void setEmail1(String email1);

	public String getTitle();

	public void setTitle(String title);

	public String getDateModified();

	public void setDateModified(String date);

	public String getStreet();

	public void setStreet(String street);

	public String getCity();

	public void setCity(String city);

	public String getPostalCode();

	public void setPostalCode(String postalCode);

	public String getRegion();

	public void setRegion(String region);

	public String getCountry();

	public void setCountry(String country);

	public String get(String field);

	public void set(String field, String value);

	public String getDisplayName();
}
