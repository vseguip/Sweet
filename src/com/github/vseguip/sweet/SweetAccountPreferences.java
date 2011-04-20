/********************************************************************\

File: SweetAccountPreferences.java
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

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

public class SweetAccountPreferences extends PreferenceActivity {
		
		public static final String TAG = "SweetAccountPreferences";
		private boolean shouldForceSync = false;

		@Override
		public void onCreate(Bundle icicle) {
		    super.onCreate(icicle);
		    Log.i(TAG, "onCreate");
		    addPreferencesFromResource(R.xml.account_preferences_resources);
		}

		@Override
		public void onPause() {
		    super.onPause();
		    if (shouldForceSync) {
		        //AccountAuthenticatorService.resyncAccount(this);
		    }
		}

		Preference.OnPreferenceChangeListener syncToggle = new Preference.OnPreferenceChangeListener() {
		    public boolean onPreferenceChange(Preference preference, Object newValue) {
		        shouldForceSync = true;
		        return true;
		    }
		};
}
