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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.auth.AuthenticationException;

import com.github.vseguip.sweet.R;
import com.github.vseguip.sweet.SweetAuthenticatorActivity;
import com.github.vseguip.sweet.rest.SugarAPI;
import com.github.vseguip.sweet.rest.SugarAPIFactory;
import com.github.vseguip.sweet.rest.SweetContact;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
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
	private String mCurrentId;
	private int mPosResolved;
	private Button mButtonNextConflict;
	private Button mButtonPrevConflict;
	private Account mAccount;
	private ArrayList<String> mConflictSet;
	private AccountManager mAccountManager;
	private boolean mPreferServer;
	private SyncResolvedContactsTask task = null;

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
		mAccountManager = AccountManager.get(this);
		final Map<String, ISweetContact> localContacts = new HashMap<String, ISweetContact>();
		final Map<String, ISweetContact> sugarContacts = new HashMap<String, ISweetContact>();
		try {
		retreiveConflicts(localContacts, sugarContacts);
		} catch(Exception ex) {
			Log.e(TAG, "This shouldn't happen " + ex.getMessage());
			ex.printStackTrace();
			quitResolver();
		}
		final TableLayout fieldTable = (TableLayout) findViewById(R.id.fieldTable);
		mButtonPrevConflict = (Button) findViewById(R.id.buttonPreviousConflict);
		mButtonNextConflict = (Button) findViewById(R.id.buttonNextConflict);
		Button buttonCancel = (Button) findViewById(R.id.buttonCancel);
		Button buttonResolve = (Button) findViewById(R.id.buttonResolve);
		mAccount = getIntent().getParcelableExtra("account");
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		mPreferServer = settings.getBoolean(this.getString(R.string.prefer_server_resolve), false);
		
		task = (SyncResolvedContactsTask) getLastNonConfigurationInstance();

		if (task == null) {
			task = new SyncResolvedContactsTask(this, mAccountManager, mAccount, getString(R.string.account_type));			
		} else {
			task.attach(this);
		}

		
		buttonCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				quitResolver();
			}
		});

		buttonResolve.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				doResolve();

			}
		});
		if ((localContacts.size() >= 0) && (sugarContacts.size() == localContacts.size())) {
			mConflictSet = new ArrayList<String>(localContacts.keySet());
			createResolvedContactsArray(localContacts, sugarContacts);

			mPosResolved = 0;
			mCurrentId = mConflictSet.get(mPosResolved);

			displayConflict(localContacts, sugarContacts, fieldTable);

			mButtonNextConflict.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if ((mPosResolved < mConflictSet.size()) && (mPosResolved < resolvedContacts.length)) {
						mPosResolved++;
						mCurrentId = mConflictSet.get(mPosResolved);
						displayConflict(localContacts, sugarContacts, fieldTable);

					}
				}
			});
			mButtonPrevConflict.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (mPosResolved > 0) {
						mPosResolved--;
						mCurrentId = mConflictSet.get(mPosResolved);
						displayConflict(localContacts, sugarContacts, fieldTable);
					}
				}
			});

		} else {
			quitResolver();
			return;
		}

	}

	/**
	 * Initializes the array of resolved contacts merging field whenever
	 * possible
	 * 
	 * @param localContacts
	 *            Local versions of the contacts
	 * @param sugarContacts
	 *            Remote version of the contact
	 */
	private void createResolvedContactsArray(final Map<String, ISweetContact> localContacts,
			final Map<String, ISweetContact> sugarContacts) {
		resolvedContacts = new SweetContact[localContacts.size()];
		for (int i = 0; i < resolvedContacts.length; i++) {
			String idContact = mConflictSet.get(i);
			if (mPreferServer) {
				resolvedContacts[i] = sugarContacts.get(idContact).mergeContact(localContacts.get(idContact));
			} else {
				resolvedContacts[i] = localContacts.get(idContact).mergeContact(sugarContacts.get(idContact));
			}
		}
	}

	/**
	 * @param localContacts
	 * @param sugarContacts
	 */
	private void displayConflict(Map<String, ISweetContact> localContacts, Map<String, ISweetContact> sugarContacts,
			TableLayout fieldTable) {
		if (mPosResolved == 0) {
			mButtonPrevConflict.setVisibility(View.GONE);
		} else {
			mButtonPrevConflict.setVisibility(View.VISIBLE);
		}
		if (mPosResolved == (resolvedContacts.length - 1)) {
			mButtonNextConflict.setVisibility(View.GONE);
		} else {
			mButtonNextConflict.setVisibility(View.VISIBLE);
		}
		mCurrentLocal = localContacts.get(mCurrentId);
		mCurrentSugar = sugarContacts.get(mCurrentId);
		TextView titleView = (TextView) findViewById(R.id.textConflictName);
		titleView.setText(getString(R.string.resolve_conflict) + " (" + (mPosResolved + 1) + " of "
				+ localContacts.size() + ")" + "\n" + mCurrentLocal.getDisplayName());
		fieldTable.removeAllViews();
		for (int i = 0; i < ISweetContact.COMPARISON_FIELDS.length; i++) {
			String field = ISweetContact.COMPARISON_FIELDS[i];
			String localVal = mCurrentLocal == null ? "" : mCurrentLocal.get(field);
			String sugarVal = mCurrentSugar == null ? "" : mCurrentSugar.get(field);
			if (localVal == null)
				localVal = "";
			if (sugarVal == null)
				sugarVal = "";
			if (!localVal.equals(sugarVal))
				addConflictRow(fieldTable, field, localVal, sugarVal);
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
	 * @param nameOfField
	 * @param field
	 */
	private void addConflictRow(TableLayout fieldTable, final String nameOfField, final String fieldLocal,
			final String fieldRemote) {
		if (mCurrentLocal == null || mCurrentSugar == null)
			return;
		// String fieldLocal = mCurrentLocal.get(nameOfField);
		// String fieldRemote = mCurrentSugar.get(nameOfField);
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
		int stringId = this.getResources().getIdentifier(nameOfField, "string", this.getPackageName());
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
		sourceSelect.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position == 0) {
					fieldValueLocal.setTextAppearance(SweetConflictResolveActivity.this, R.style.textSelected);
					fieldValueRemote.setTextAppearance(SweetConflictResolveActivity.this, R.style.textUnselected);
					resolvedContacts[mPosResolved].set(nameOfField, fieldLocal);
				} else {
					fieldValueLocal.setTextAppearance(SweetConflictResolveActivity.this, R.style.textUnselected);
					fieldValueRemote.setTextAppearance(SweetConflictResolveActivity.this, R.style.textSelected);
					resolvedContacts[mPosResolved].set(nameOfField, fieldRemote);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> view) {
			}
		});
		row.setPadding(row.getLeft(), row.getTop() + 5, row.getRight(), row.getBottom() + 10);
		// Restore appropiate selections according to resolved contact
		if (resolvedContacts[mPosResolved].get(nameOfField).equals(fieldLocal)) {
			sourceSelect.setSelection(0);
		} else {
			sourceSelect.setSelection(1);
		}
		fieldTable.addView(row);
	}

	@SuppressWarnings("unchecked")
	private void doResolve() {
		Log.i(TAG, "Saving resolved data");
		List<ISweetContact> contacts = new ArrayList<ISweetContact>();
		for (int i = 0; i < resolvedContacts.length; i++) {
			// do a deep copy before sending to the async task, guarantees no
			// concurrency issues
			contacts.add(resolvedContacts[i].deepCopy());
		}

		if (contacts.size() > 0) {
			task.execute(contacts);
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		task.detach();

		return (task);
	}

	private static class SyncResolvedContactsTask extends AsyncTask<List<ISweetContact>, Integer, Long> {
		SweetConflictResolveActivity mActivity = null;
		AccountManager mAccountManager;
		Account mAccount;
		private String mAccountType;

		public SyncResolvedContactsTask(SweetConflictResolveActivity activity, AccountManager accountManager,
				Account account, String accountType) {
			attach(activity);
			mAccountManager = accountManager;
			mAccount = account;
			mAccountType = accountType;
		}

		protected Long doInBackground(List<ISweetContact>... contactsA) {
			Long result = 0L;
			// Save locally
			for (int i = 0; i < contactsA.length; i++) {
				List<ISweetContact> contacts = contactsA[i];
				String lastDate = mAccountManager.getUserData(mAccount, SweetContactSync.LAST_SYNC_KEY);
				String server = mAccountManager.getUserData(mAccount, SweetAuthenticatorActivity.KEY_PARAM_SERVER);
				Log.i(TAG, "Local update");
				if (mActivity != null) {
					ContactManager.syncContacts(mActivity, mAccount, contacts);
					Log.i(TAG, "Remote update");
					for (ISweetContact c : contacts) {
						String contactDate = c.getDateModified();
						if ((lastDate == null) || (lastDate.compareTo(contactDate) < 0)) {
							lastDate = contactDate;
						}
					}

					SugarAPI sugar;
					try {
						sugar = SugarAPIFactory.getSugarAPI(server);
						String authToken = mAccountManager.blockingGetAuthToken(mAccount, mAccountType, true);
						if (authToken == null) {
							// Maybe session expired? retry...
							authToken = mAccountManager.blockingGetAuthToken(mAccount, mAccountType, true);
						}
						// Set false flag, don't create new users remotely!
						List<String> newIds = sugar.sendNewContacts(authToken, contacts, false);
						result += (newIds.size() - contacts.size());
						if (newIds.size() == contacts.size()) {
							mAccountManager.setUserData(mAccount, SweetContactSync.LAST_SYNC_KEY, lastDate);
						}

					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (OperationCanceledException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (AuthenticatorException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (AuthenticationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			return result;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {

		}

		@Override
		protected void onPostExecute(Long result) {
			if (mActivity != null) {
				if (result == 0L) {
					Toast.makeText(mActivity, R.string.conflict_resolved, Toast.LENGTH_LONG).show();

				} else {
					Toast.makeText(mActivity, R.string.conflict_error, Toast.LENGTH_LONG).show();
				}
				mActivity.quitResolver();
			}

			return;
		}

		void detach() {
			mActivity = null;
		}

		void attach(SweetConflictResolveActivity activity) {
			this.mActivity = activity;
		}

	}

}
