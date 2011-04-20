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

import java.net.URISyntaxException;

import android.content.Context;
import android.os.Handler;

public interface SugarAPI {
	public abstract void setServer(String server) throws URISyntaxException ;
	public abstract boolean validate(String username, String passwd, Context context, Handler handler);

}