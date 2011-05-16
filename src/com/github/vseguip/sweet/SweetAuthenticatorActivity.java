package com.github.vseguip.sweet;

import java.net.URISyntaxException;
import com.github.vseguip.sweet.rest.SugarAPI;
import com.github.vseguip.sweet.rest.SugarAPIFactory;
import com.github.vseguip.sweet.utils.Utils;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class SweetAuthenticatorActivity extends AccountAuthenticatorActivity {
	/**
	 * The key to account metadata. Holds the version of the account (how
	 * information is mapped, etc)
	 */
	public static final String KEY_SYNC_VERSION = "syncVersion";

	public static final String KEY_PARAM_SERVER = "server";
	public static final String KEY_PARAM_VALIDATE = "noCertificateValidation";
	public static final String KEY_PARAM_ENCRYPT = "encryptPasswd";
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

	private TextView mTextAction;

	private CheckBox mCheckCerts;

	private CheckBox mCheckEncrypt;

	private String mUsername;

	private String mPasswd;

	private String mServer;

	private boolean mNoValidate;

	private boolean mEncrypt;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate(" + savedInstanceState + ")");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_LEFT_ICON);

		setContentView(R.layout.main);

		getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, android.R.drawable.ic_dialog_alert);

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
		mCheckCerts = (CheckBox) findViewById(R.id.checkAcceptCerts);
		mCheckEncrypt = (CheckBox) findViewById(R.id.checkEncryptPassword);
		mViewF = (ViewFlipper) findViewById(R.id.ViewFlipper01);
		mButtonValidate = (Button) findViewById(R.id.buttonValidate);
		mButtonBack = (Button) findViewById(R.id.buttonBack);
		mButtonCancel = (Button) findViewById(R.id.buttonCancel);
		mButtonCreate = (Button) findViewById(R.id.buttonCreate);
		mTextAction = (TextView) findViewById(R.id.textViewCreate);
		if (mCreateAccount) {
			Account[] accounts = mAccountManager.getAccountsByType(getString(R.string.account_type));
			if ((accounts != null) && (accounts.length > 0)) {
				Toast.makeText(this, R.string.only_one_account, Toast.LENGTH_LONG).show();
				finish();
				return;
			}
			mCheckCerts.setChecked(true);
			mCheckEncrypt.setChecked(true);
			mTextAction.setText(R.string.activity_create);
		} else {
			mTextAction.setText(R.string.activity_check_update);
		}
		if (!mConfirmCredentials && !mCreateAccount) {
			String username = intent.getStringExtra(PARAM_USERNAME);
			Account acc = new Account(username, ACCOUNT_TYPE);
			String server = mAccountManager.getUserData(acc, KEY_PARAM_SERVER);
			boolean validate = Utils.getBooleanAccountData(mAccountManager, acc, KEY_PARAM_VALIDATE, true);
			boolean encrypt = Utils.getBooleanAccountData(mAccountManager, acc, KEY_PARAM_ENCRYPT, true);
			mUserEdit.setText(username);
			mPasswdEdit.setText(intent.getStringExtra(PARAM_PASSWORD));
			mServerEdit.setText(server);
			mCheckCerts.setChecked(validate);
			mCheckEncrypt.setChecked(encrypt);
		}
		mButtonValidate.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getUIAccountData();
				if (mUsername == null || mUsername.length() <= 0) {
					Toast.makeText(SweetAuthenticatorActivity.this, "Please enter a valid username", Toast.LENGTH_LONG);
					return;
				}
				if (mPasswd == null || mPasswd.length() <= 0) {
					Toast.makeText(SweetAuthenticatorActivity.this, "Please enter a valid passwd", Toast.LENGTH_LONG);
					return;
				}
				if (mServer == null || mServer.length() <= 0) {
					Toast.makeText(
									SweetAuthenticatorActivity.this,
									"Please enter a valid URL to use as a server resource",
									Toast.LENGTH_LONG);
					return;
				}
				try {
					Account account = new Account(mUsername, ACCOUNT_TYPE);
					SugarAPI sugar;
					if (mCreateAccount) {					
						sugar = SugarAPIFactory.getSugarAPI(mServer, mNoValidate, mEncrypt);
					} else {
						setAccountData(account);
						sugar = SugarAPIFactory.getSugarAPI(mAccountManager, account);
					}
					sugar.getToken(mUsername, mPasswd, SweetAuthenticatorActivity.this, handler);
				} catch (URISyntaxException ex) {
					Toast.makeText(
									SweetAuthenticatorActivity.this,
									"Please enter a valid URI to use as a server resource",
									Toast.LENGTH_LONG);
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
		getUIAccountData();
		final Account account = new Account(mUsername, ACCOUNT_TYPE);
		setAccountData(account);
		final Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, true);
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		Toast.makeText(this, getString(R.string.succesful_authentication), Toast.LENGTH_LONG).show();

		finish();

	}

	protected void createAccount() {
		Log.i(TAG, "createAccount()");
		getUIAccountData();
		final Account account = new Account(mUsername, ACCOUNT_TYPE);
		if (mCreateAccount) {
			Bundle serverData = new Bundle();
			mAccountManager.addAccountExplicitly(account, mPasswd, serverData);
			setAccountData(account);
			// Set contacts sync for this account.
			ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
			ContentProviderClient client = getContentResolver()
					.acquireContentProviderClient(ContactsContract.AUTHORITY_URI);
			ContentValues values = new ContentValues();
			values.put(ContactsContract.Groups.ACCOUNT_NAME, account.name);
			values.put(Groups.ACCOUNT_TYPE, account.type);
			values.put(Settings.UNGROUPED_VISIBLE, true);
			try {
				client.insert(Settings.CONTENT_URI.buildUpon()
						.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build(), values);
			} catch (RemoteException e) {
				e.printStackTrace();
			}

		} else {
			setAccountData(account);
		}
		final Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername);
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, ACCOUNT_TYPE);
		intent.putExtra(AccountManager.KEY_PASSWORD, mPasswd);
		intent.putExtra(AccountManager.KEY_USERDATA, mServer);
		if (mAuthtokenType != null && mAuthtokenType.equals(ACCOUNT_TYPE)) {
			intent.putExtra(AccountManager.KEY_AUTHTOKEN, mIdSession);
		}
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();
	}

	/**
	 * Get account data from the UI
	 * 
	 */
	private void getUIAccountData() {
		mUsername = mUserEdit.getText().toString();
		mPasswd = mPasswdEdit.getText().toString();
		mServer = mServerEdit.getText().toString();
		mNoValidate = mCheckCerts.isChecked();
		mEncrypt = mCheckEncrypt.isChecked();
	}

	/**
	 * Sets the account data (server, ssl support and password encryption)
	 * 
	 * @param account
	 *            The account to set the data for
	 * 
	 */
	private void setAccountData(final Account account) {

		mAccountManager.setUserData(account, KEY_PARAM_SERVER, mServer);
		mAccountManager.setUserData(account, KEY_PARAM_VALIDATE, Boolean.toString(mNoValidate));
		mAccountManager.setUserData(account, KEY_PARAM_ENCRYPT, Boolean.toString(mEncrypt));
		// set the version of the data mapping used for
		// contacts/calendars/etc
		mAccountManager.setPassword(account, mPasswd);
		mAccountManager.setUserData(account, KEY_SYNC_VERSION, getString(R.string.sync_version));
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
				confirmCredentials();

			}
		} else {
			Toast.makeText(this, message, Toast.LENGTH_LONG).show();
		}
	}
}