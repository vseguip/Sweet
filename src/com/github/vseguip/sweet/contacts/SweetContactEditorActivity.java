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
import com.github.vseguip.sweet.rest.SweetContact;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SweetContactEditorActivity extends Activity {
	private static final String TAG = "SweetContactEditorActivity";
	private EditText mEditFirstName;
	private EditText mEditLastName;
	private TextView mTextTitleName;
	private EditText mOrganization;
	private EditText mTitle;
	private EditText mWorkPhone;
	private EditText mFax;
	private EditText mMobilePhone;
	private EditText mStreet;
	private EditText mCity;
	private EditText mPostalCode;
	private EditText mRegion;
	private EditText mCountry;
	private TextView mEmail1;
	private ISweetContact mContact;
	private Button mSaveButton;
	private boolean mForceCreate;
	private Account mAccount;
	private Uri mContactUri;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate()");
		
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.contact_editor);
		mContact = null;
		mTextTitleName = (TextView) findViewById(R.id.textTitleName);
		mEditFirstName = (EditText) findViewById(R.id.textFirstName);
		mEditLastName = (EditText) findViewById(R.id.textLastName);
		mOrganization = (EditText) findViewById(R.id.textOrganization);
		mTitle = (EditText) findViewById(R.id.textTitle);

		mWorkPhone = (EditText) findViewById(R.id.textWorkPhone);
		mMobilePhone = (EditText) findViewById(R.id.textMobilePhone);
		mFax = (EditText) findViewById(R.id.textFaxNumber);
		mEmail1 = (EditText) findViewById(R.id.textEmail);
		mStreet = (EditText) findViewById(R.id.textStreet);
		mCity = (EditText) findViewById(R.id.textCity);
		mPostalCode = (EditText) findViewById(R.id.textPostalCode);
		mRegion = (EditText) findViewById(R.id.textRegion);
		mCountry = (EditText) findViewById(R.id.textCountry);

		mSaveButton = (Button) findViewById(R.id.buttonSave);
		Button cancelButton = (Button) findViewById(R.id.buttonCancel);

		mSaveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SweetContactEditorActivity.this.saveContact();
			}
		});

		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SweetContactEditorActivity.this.finish();
			}
		});

		mEditFirstName.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				SweetContactEditorActivity.this.validateNotEmpty(mEditFirstName);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		mEditLastName.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				SweetContactEditorActivity.this.validateNotEmpty(mEditLastName);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		Intent intent = getIntent();
		String action = intent.getAction();
		
		if(ContactsContract.Intents.Insert.ACTION.equals(action)){
			
			AccountManager am = AccountManager.get(this);
			Account[] accounts = am.getAccountsByType(getString(R.string.account_type));
			if(accounts==null || accounts.length==0){
				//no matching account type, quit
				Toast.makeText(this, R.string.error_no_account, Toast.LENGTH_LONG).show();				
				finish();
				return;
			}
			mAccount = accounts[0];
			if(accounts.length > 1){
				pickAccount(accounts);
			}
			mContactUri = null;
			mForceCreate=true;
			mContact = new SweetContact();
			displayContactData();
		}
		else if (Intent.ACTION_VIEW.equals(action)) {
			mContactUri = intent.getData();
			mAccount = null;		
			mContact = ContactManager.getContactFromMime(this, mContactUri);
			mForceCreate=false;
			displayContactData();

		}

	}

	private Account pickAccount(final Account[] accounts) {
		String accountNames[] = new String[accounts.length]; 
		for (int i = 0; i < accountNames.length; i++) {
			accountNames[i] = accounts[i].name;
		}
		AlertDialog dialog = new AlertDialog.Builder(this).setItems(accountNames, new DialogInterface.OnClickListener() {			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mAccount = accounts[which];				
			}
		}).setTitle(R.string.choose_account).setOnCancelListener(new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				SweetContactEditorActivity.this.finish();
				
			}
		}).create();
		dialog.show();
		
		return mAccount;
	}

	/**
	 * 
	 */
	private void displayContactData() {
		if (mContact != null) {
			mEditFirstName.setText(mContact.getFirstName());
			mEditLastName.setText(mContact.getLastName());
			mTextTitleName.setText(mContact.getFirstName() + " " + mContact.getLastName());
			mOrganization.setText(mContact.getAccountName());
			mTitle.setText(mContact.getTitle());

			mWorkPhone.setText(mContact.getWorkPhone());
			mMobilePhone.setText(mContact.getMobilePhone());
			mFax.setText(mContact.getWorkFax());
			mEmail1.setText(mContact.getEmail1());

			mStreet.setText(mContact.getStreet());
			mCity.setText(mContact.getCity());
			mPostalCode.setText(mContact.getPostalCode());
			mRegion.setText(mContact.getRegion());
			mCountry.setText(mContact.getCountry());
		}
	}

	private void getContactData() {
		if ((mContact != null) && validateNotEmpty(mEditFirstName) && validateNotEmpty(mEditLastName)) {
			mContact.setFirstName(mEditFirstName.getText().toString());
			mContact.setLastName(mEditLastName.getText().toString());			
			
			mContact.setTitle(mTitle.getText().toString());

			mContact.setWorkPhone(mWorkPhone.getText().toString());
			mContact.setMobilePhone(mMobilePhone.getText().toString());
			mContact.setWorkFax(mFax.getText().toString());
			mContact.setEmail1(mEmail1.getText().toString());

			mContact.setStreet(mStreet.getText().toString());
			mContact.setCity(mCity.getText().toString());
			mContact.setPostalCode(mPostalCode.getText().toString());
			
			mContact.setRegion(mRegion.getText().toString());
			mContact.setCountry(mCountry.getText().toString());
		}
	}

	private void saveContact() {
		if (mContact != null) {
			getContactData();//retrieve data from fields
			ContactManager.createOrUpdateContact(this, mContact, mAccount, mContactUri, mForceCreate);
		}
		finish();
	}

	private boolean validateNotEmpty(EditText edit) {
		if (TextUtils.isEmpty(edit.getText()) || TextUtils.isEmpty(edit.getText().toString().trim())) {
			mSaveButton.setEnabled(false);
			edit.setBackgroundResource(R.drawable.red_underline);
			return false;
		} else {
			mSaveButton.setEnabled(true);
			edit.setBackgroundResource(R.drawable.blue_underline);
			return true;
		}
	}
}
