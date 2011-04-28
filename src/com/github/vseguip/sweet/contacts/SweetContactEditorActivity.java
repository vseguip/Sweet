/********************************************************************\

File: ContactEditorActivity.java
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

import com.github.vseguip.sweet.R;

import android.app.Activity;
import android.app.TabActivity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContactsEntity;
import android.util.Log;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TextView;

public class SweetContactEditorActivity extends Activity {
	private static final String TAG = "SweetContactEditorActivity";
	private EditText mEditFirstName;
	private EditText mEditLastName;
	private TextView mTextTitleName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate()");
		setContentView(R.layout.contact_editor);
		mTextTitleName= (TextView) findViewById(R.id.textTitleName);
		mEditFirstName = (EditText) findViewById(R.id.textFirstName);
		mEditLastName = (EditText) findViewById(R.id.textLastName);
		Intent intent = getIntent();
		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			Uri  entityUri = intent.getData();
			
			
			ISweetContact contact = ContactManager.getContactFromMime(this, entityUri);
			mEditFirstName.setText(contact.getFirstName());
			mEditLastName.setText(contact.getLastName());
			mTextTitleName.setText(contact.getFirstName()+ " "+ contact.getLastName());
		}
	}


}
