package com.github.vseguip.sweet;

import java.net.URISyntaxException;
import com.github.vseguip.sweet.rest.SugarAPI;
import com.github.vseguip.sweet.rest.SugarAPIFactory;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class SweetAuthenticatorActivity extends AccountAuthenticatorActivity {

	public static final String KEY_PARAM_SERVER = "server";
	/** The Intent flag to confirm credentials. **/
	public static final String PARAM_CONFIRM_CREDENTIALS = "confirmCredentials";
	/** The Intent extra to store password. **/
	public static final String PARAM_PASSWORD = "password";
	/** The Intent extra to store username. **/
	public static final String PARAM_USERNAME = "username";
	/** The Intent extra to store authtoken type. **/
	public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";
	/**
	 * The Intent extra to signal if we want to create an account or just
	 * confirm credentials.
	 **/
	public static final String PARAM_CREATE_ACCOUNT = "createAccount";
	public static final String TAG = "AccountAuthenticatorActivity";
	private String ACCOUNT_TYPE;
	final Handler handler = new Handler();
	private boolean mCreateAccount;
	private boolean mConfirmCredentials;
	private EditText mUserEdit;
	private EditText mPasswdEdit;
	private EditText mServerEdit;
	private Button mButtonValidate;
	private ViewFlipper mViewF;
	private Button mButtonBack;
	private Button mButtonCancel;
	private Button mButtonCreate;
	private AccountManager mAccountManager;
	private String mIdSession;
	private Object mAuthtokenType;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate(" + savedInstanceState + ")");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Log.e(TAG, "Content authority: " + ContactsContract.AUTHORITY);
		ACCOUNT_TYPE = getString(R.string.account_type);
		final Intent intent = getIntent();
		mAccountManager = AccountManager.get(this);
		// We must create the account
		mCreateAccount = intent.getBooleanExtra(PARAM_CREATE_ACCOUNT, false);
		// We must confirm the credentials
		mConfirmCredentials = intent.getBooleanExtra(PARAM_CONFIRM_CREDENTIALS, false);
		mAuthtokenType = intent.getStringExtra(PARAM_AUTHTOKEN_TYPE);// Tipo de
		// token
		// a
		// crear

		mUserEdit = (EditText) findViewById(R.id.EditUserName);
		mPasswdEdit = (EditText) findViewById(R.id.EditPassword);
		mServerEdit = (EditText) findViewById(R.id.EditServer);
		mViewF = (ViewFlipper) findViewById(R.id.ViewFlipper01);
		mButtonValidate = (Button) findViewById(R.id.buttonValidate);
		mButtonBack = (Button) findViewById(R.id.buttonBack);
		mButtonCancel = (Button) findViewById(R.id.buttonCancel);
		mButtonCreate = (Button) findViewById(R.id.buttonCreate);
		mButtonValidate.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String username;
				String passwd;
				String server;
				username = mUserEdit.getText().toString();
				passwd = mPasswdEdit.getText().toString();
				server = mServerEdit.getText().toString();
				if (username == null || username.length() <= 0) {
					Toast.makeText(SweetAuthenticatorActivity.this, "Please enter a valid username", Toast.LENGTH_LONG);
					return;
				}
				if (passwd == null || passwd.length() <= 0) {
					Toast.makeText(SweetAuthenticatorActivity.this, "Please enter a valid passwd", Toast.LENGTH_LONG);
					return;
				}
				if (server == null || server.length() <= 0) {
					Toast.makeText(SweetAuthenticatorActivity.this,
							"Please enter a valid URL to use as a server resource", Toast.LENGTH_LONG);
					return;
				}
				try {
					SugarAPI sugar = SugarAPIFactory.getSugarAPI(server);
					sugar.setServer(server);
					sugar.getToken(username, passwd, SweetAuthenticatorActivity.this, handler);
				} catch (URISyntaxException ex) {
					Toast.makeText(SweetAuthenticatorActivity.this,
							"Please enter a valid URI to use as a server resource", Toast.LENGTH_LONG);
				}
			}
		});
		mButtonBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mViewF.showPrevious();
			}
		});
		mButtonCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SweetAuthenticatorActivity.this.finish();
			}
		});
		mButtonCreate.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SweetAuthenticatorActivity.this.createAccount();
			}
		});
	}

	protected void confirmCredentials() {
		Log.i(TAG, "finishConfirmCredentials()");
		String username = mUserEdit.getText().toString();
		String passwd = mPasswdEdit.getText().toString();
		String server = mServerEdit.getText().toString();
		final Account account = new Account(username, ACCOUNT_TYPE);
		mAccountManager.setPassword(account, passwd);
		mAccountManager.setUserData(account, KEY_PARAM_SERVER, server);
		final Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, true);
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();

	}

	protected void createAccount() {
		Log.i(TAG, "createAccount()");
		String username = mUserEdit.getText().toString();
		String passwd = mPasswdEdit.getText().toString();
		String server = mServerEdit.getText().toString();

		final Account account = new Account(username, ACCOUNT_TYPE);
		if (mCreateAccount) {
			Bundle serverData = new Bundle();
			//serverData.putString(KEY_PARAM_SERVER, server);
			mAccountManager.addAccountExplicitly(account, passwd, serverData);
			mAccountManager.setUserData(account, KEY_PARAM_SERVER, server);
			// Set contacts sync for this account.
			ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
		} else {
			mAccountManager.setPassword(account, passwd);
		}
		final Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, username);
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, ACCOUNT_TYPE);
		intent.putExtra(AccountManager.KEY_PASSWORD, passwd);
		intent.putExtra(AccountManager.KEY_USERDATA, server);
		if (mAuthtokenType != null && mAuthtokenType.equals(ACCOUNT_TYPE)) {
			intent.putExtra(AccountManager.KEY_AUTHTOKEN, mIdSession);
		}
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();
	}

	public void onValidationResult(boolean result, String message) {
		if (result == true) {
			// Validation was successful, show page to confirm account creation
			// message carries the ID token the SugarCRM sent us back. Save it
			// for account creation
			mIdSession = message;
			if (mCreateAccount) {
				mViewF.showNext();
			} else {
				if (mConfirmCredentials) {
					confirmCredentials();
				}
			}
		} else {
			Toast.makeText(this, message, Toast.LENGTH_LONG).show();
		}
	}
}