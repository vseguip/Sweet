/********************************************************************\

File: SweetConflictResolveActivity.java
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.github.vseguip.sweet.R;
import com.github.vseguip.sweet.rest.SweetContact;

import android.app.Activity;
import android.app.NotificationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class SweetConflictResolveActivity extends Activity {
	private final static String TAG = "SweetConflictResolveActivity";
	public final static String NOTIFY_CONFLICT = "SweetConflictResolveActivity.ResolveConflict";
	public final static int NOTIFY_CONTACT = 1;

	private static Map<String, ISweetContact> conflictingLocalContacts;
	private static Map<String, ISweetContact> conflictingSugarContacts;
	private ISweetContact mCurrentLocal;
	private ISweetContact mCurrentSugar;
	private ISweetContact[] resolvedContacts;
	/**
	 * Stores the list of conflicting contacts to avoid having to serialize
	 * within the intent
	 * 
	 * @param _conflictingLocalContacts
	 *            Map of local contacts
	 * @param _conflictingSugarContacts
	 *            Map of remote contacts
	 * 
	 * 
	 */
	public static synchronized void storeConflicts(Map<String, ISweetContact> _conflictingLocalContacts,
			Map<String, ISweetContact> _conflictingSugarContacts) {
		conflictingLocalContacts = _conflictingLocalContacts;
		conflictingSugarContacts = _conflictingSugarContacts;
	}

	/**
	 * Retreive the list of conflicting contacts to avoid having to serialize
	 * within the intent
	 * 
	 * @param _conflictingLocalContacts
	 *            Map of local contacts
	 * @param _conflictingSugarContacts
	 *            Map of remote contacts
	 * 
	 * 
	 */

	public static synchronized void retreiveConflicts(Map<String, ISweetContact> localContacts,
			Map<String, ISweetContact> sugarContacts) {
		localContacts.putAll(conflictingLocalContacts);
		sugarContacts.putAll(conflictingSugarContacts);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate()");
		// getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.conflict_resolver);
		TableLayout fieldTable = (TableLayout) findViewById(R.id.fieldTable);
		Map<String, ISweetContact> localContacts = new HashMap<String, ISweetContact>();
		Map<String, ISweetContact> sugarContacts = new HashMap<String, ISweetContact>();
		retreiveConflicts(localContacts, sugarContacts);
		if ((localContacts.size() >= 0) && (sugarContacts.size() == localContacts.size())) {
			resolvedContacts = new SweetContact[localContacts.size()];
			for (int i = 0; i < resolvedContacts.length; i++) {
				resolvedContacts[i]=new SweetContact();
			}
			Set<String> conflictSet = localContacts.keySet();
			Iterator<String> currentIterator = conflictSet.iterator();
			int posResolved = 0;
			
			String currentId = currentIterator.next();			
			mCurrentLocal = localContacts.get(currentId);
			mCurrentSugar = sugarContacts.get(currentId);
			TextView titleView = (TextView) findViewById(R.id.textConflictName);
			titleView.setText(getString(R.string.resolve_conflict) + " ("+ (posResolved+1)+ " of " + localContacts.size() + ")" + "\n"
					+ mCurrentLocal.getDisplayName());
			resolvedContacts[posResolved].setId(mCurrentSugar.getId());
			resolvedContacts[posResolved].setDateModified(mCurrentSugar.getDateModified());
			resolvedContacts[posResolved].setAccountId(mCurrentSugar.getAccountId());
			for (int i = 0; i < ISweetContact.COMPARISON_FIELDS.length; i++) {
				String field = ISweetContact.COMPARISON_FIELDS[i];
				resolvedContacts[posResolved].set(field, mCurrentLocal.get(field));
				if (!mCurrentLocal.get(field).equals(mCurrentSugar.get(field)))
					addConflictRow(fieldTable, field);

			}
		} else {
			quitResolver();
			return;
		}

	}

	/**
	 * 
	 */
	private void quitResolver() {
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(NOTIFY_CONFLICT, NOTIFY_CONTACT);
		finish();
	}

	/**
	 * @param fieldTable
	 * @param name
	 * @param field
	 */
	private void addConflictRow(TableLayout fieldTable, final String name) {
		if (mCurrentLocal == null || mCurrentSugar == null)
			return;
		String fieldLocal = mCurrentLocal.get(name);
		String fieldRemote = mCurrentSugar.get(name);
		TableRow row = new TableRow(this);
		final Spinner sourceSelect = new Spinner(this);
		sourceSelect.setBackgroundResource(R.drawable.black_underline);
		ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
				this.getResources().getStringArray(R.array.conflict_sources));
		spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sourceSelect.setAdapter(spinnerArrayAdapter);
		// Open the spinner when pressing any of the text fields
		OnClickListener spinnerOpener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				sourceSelect.performClick();
			}
		};
		row.addView(sourceSelect);
		fieldTable.addView(row);
		row = new TableRow(this);
		TextView fieldName = new TextView(this);
		int stringId = this.getResources().getIdentifier(name, "string", this.getPackageName());
		fieldName.setText(this.getString(stringId));
		fieldName.setTextSize(16);
		fieldName.setPadding(
								fieldName.getPaddingLeft(),
								fieldName.getPaddingTop(),
								fieldName.getPaddingRight() + 10,
								fieldName.getPaddingBottom());
		fieldName.setOnClickListener(spinnerOpener);
		row.addView(fieldName);
		final TextView fieldValueLocal = new TextView(this);
		fieldValueLocal.setText(fieldLocal);
		fieldValueLocal.setTextSize(16);
		row.addView(fieldValueLocal);
		fieldValueLocal.setOnClickListener(spinnerOpener);

		fieldTable.addView(row);
		row = new TableRow(this);
		row.addView(new TextView(this));// add dummy control
		final TextView fieldValueRemote = new TextView(this);
		fieldValueRemote.setText(fieldRemote);
		fieldValueRemote.setTextSize(16);

		fieldValueRemote.setOnClickListener(spinnerOpener);
		row.addView(fieldValueRemote);
//		sourceSelect.setTag(R.string.spinner_tag_field, name);
//		sourceSelect.setTag(R.string.spinner_tag_local, fieldValueLocal);
//		sourceSelect.setTag(R.string.spinner_tag_remote, fieldValueRemote);
		sourceSelect.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				
				if (position == 0) {
					fieldValueLocal.setTextAppearance(SweetConflictResolveActivity.this, R.style.textSelected);
					fieldValueRemote.setTextAppearance(SweetConflictResolveActivity.this, R.style.textUnselected);
				} else {
					fieldValueLocal.setTextAppearance(SweetConflictResolveActivity.this, R.style.textUnselected);
					fieldValueRemote.setTextAppearance(SweetConflictResolveActivity.this, R.style.textSelected);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> view) {
			}
		});
		row.setPadding(row.getLeft(), row.getTop() + 5, row.getRight(), row.getBottom() + 10);
		fieldTable.addView(row);
	}
}
