/********************************************************************\

File: SweetSyncService.java
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

import com.github.vseguip.sweet.contacts.SweetContactSync;

import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class SweetSyncService extends Service {
	private final String TAG = "SweetSyncService";
	private AbstractThreadedSyncAdapter mSyncAdapter = null;

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "inBind() " + intent.getAction());
		return getSyncAdapter().getSyncAdapterBinder();

	}

	private synchronized AbstractThreadedSyncAdapter getSyncAdapter() {
		Log.i(TAG, "getSyncAdapter()");
		if (mSyncAdapter == null) {
			mSyncAdapter = new SweetContactSync(this, true);
		}
		return mSyncAdapter;
	}
}
