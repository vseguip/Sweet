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
	
	public String getPhoneWork();
	public void setPhoneWork(String phoneWork);

	public String getEmail1();
	public void setEmail1(String email1);
	
	public String getTitle();
	public void setTitle(String title);


}
