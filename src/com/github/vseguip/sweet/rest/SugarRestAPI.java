/********************************************************************\

File: SugarRestAPI.java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.github.vseguip.sweet.SweetAuthenticatorActivity;
import com.github.vseguip.sweet.contacts.ISweetContact;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

/*Imeplementa un API para acceder a SugarCRM
 * 
 * Basado en el ejemplo de SampleSyncAdapter.
 * 
 * */
public class SugarRestAPI implements SugarAPI {

	private static final String SUGARCRM_CONTACT_ID_FIELD = "id";

	private static final String SUGARCRM_ACCOUNT_ID_FIELD = "account_id";

	private static final String SUGARCRM_DATE_MODIFIED_FIELD = "date_modified";

	private static final String SUGARCRM_PHONE_WORK_FIELD = "phone_work";

	private static final String SUGARCRM_PHONE_MOBILE_FIELD = "phone_mobile";

	private static final String SUGARCRM_FAX_WORK_FIELD = "phone_fax";

	private static final String SUGARCRM_EMAIL1_FIELD = "email1";

	private static final String SUGARCRM_ACCOUNT_NAME_FIELD = "account_name";

	private static final String SUGARCRM_TITLE_FIELD = "title";

	private static final String SUGARCRM_LAST_NAME_FIELD = "last_name";

	private static final String SUGARCRM_FIRST_NAME_FIELD = "first_name";

	private static final String SUGARCRM_STREET_FIELD = "primary_address_street";
	private static final String SUGARCRM_CITY_FIELD = "primary_address_city";
	private static final String SUGARCRM_STATE_FIELD = "primary_address_state";
	private static final String SUGARCRM_POSTAL_CODE_FIELD = "primary_address_postalcode";
	private static final String SUGARCRM_COUNTRY_FIELD = "primary_address_country";

	private static final int TIMEOUT_OPS = 30 * 1000; // ms

	private static final String KEY_PARAM_RESPONSE_TYPE = "response_type";
	private static final String KEY_PARAM_INPUT_TYPE = "input_type";
	private static final String KEY_PARAM_METHOD = "method";
	private static final String JSON = "JSON";
	private static final String TAG = "SugarRestAPI";
	private static final String LOGIN_METHOD = "login";
	private static final String GET_METHOD = "get_entry_list";
	private static final String SET_METHOD = "set_entries";

	private static final String SUGAR_MODULE_CONTACTS = "Contacts";
	private static final String SUGAR_CONTACTS_QUERY = "";
	private static final String SUGAR_CONTACT_LINK_NAMES = "";
	private static final Object SUGAR_CONTACTS_ORDER_BY = "";
	private static URI mServer;
	private HttpClient mHttpClient;

	public SugarRestAPI(String server) throws URISyntaxException {
		setServer(server);
	}

	public boolean validate(String username, String passwd, Context context, Handler handler) {
		return getToken(username, passwd, context, handler) == null;
	}

	public String getToken(String username, String passwd, Context context, Handler handler) {
		final HttpResponse resp;
		JSONObject jso_content = new JSONObject();
		try {
			JSONObject jso_user = new JSONObject();
			jso_user.put("user_name", username).put("password", encryptor(passwd));
			jso_content.put("user_auth", jso_user).put("application", "Sweet");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		final HttpPost post = prepareJSONRequest(jso_content.toString(), LOGIN_METHOD);
		HttpClient httpClient = getConnection();
		try {
			resp = httpClient.execute(post);
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				if (Log.isLoggable(TAG, Log.VERBOSE)) {
					Log.v(TAG, "Successful authentication");
				}

				// Buffer the result into a string
				BufferedReader rd = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = rd.readLine()) != null) {
					sb.append(line);
				}
				rd.close();
				String message = sb.toString();
				String authToken;
				JSONObject json = null;
				try {
					// TODO: Convert JSON to contacts
					json = (JSONObject) new JSONTokener(message).nextValue();
					authToken = json.getString("id");
				} catch (JSONException e) {
					Log.i(TAG, "Error during login" + message);
					if (json != null) {
						if (json.has("description")) {
							try {
								message = json.getString("description");// get
								// errot
								// message!
							} catch (JSONException ex) {
								e.printStackTrace();
								Log.e(TAG, "JSON exception, should never have gotten here!");
							}
							;
						}
					}
					sendResult(false, handler, context, "Error during login " + message);
					return null;
				}
				sendResult(true, handler, context, authToken);
				return authToken;
			} else {
				if (Log.isLoggable(TAG, Log.VERBOSE)) {
					Log.v(TAG, "Error authenticating" + resp.getStatusLine());
				}
				sendResult(false, handler, context, "Error authenticating" + resp.getStatusLine());
				return null;
			}
		} catch (final IOException e) {
			if (Log.isLoggable(TAG, Log.VERBOSE)) {
				Log.v(TAG, "IOException when getting authtoken", e);
			}
			sendResult(false, handler, context, "Error trying to connect to " + mServer.toString());
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			sendResult(
						false,
						handler,
						context,
						"Error trying to validate your credentials. Check you server name and net connectivity.");
			return null;
		} finally {
			if (Log.isLoggable(TAG, Log.VERBOSE)) {
				Log.v(TAG, "getAuthtoken completing");
			}
		}

	}

	@Override
	/** {@inheritDoc} */
	public List<ISweetContact> getNewerContacts(String token, String date) throws IOException, AuthenticationException {
		final HttpResponse resp;
		Log.i(TAG, "getNewerContacts()");

		JSONArray jso_array = new JSONArray();
		JSONArray jso_fields = new JSONArray();
		// TODO: add newer fields (adress and other phones)
		jso_fields.put(SUGARCRM_CONTACT_ID_FIELD).put(SUGARCRM_FIRST_NAME_FIELD).put(SUGARCRM_LAST_NAME_FIELD)
				.put(SUGARCRM_TITLE_FIELD).put(SUGARCRM_ACCOUNT_NAME_FIELD).put(SUGARCRM_ACCOUNT_ID_FIELD)
				.put(SUGARCRM_EMAIL1_FIELD).put(SUGARCRM_PHONE_WORK_FIELD).put(SUGARCRM_PHONE_MOBILE_FIELD)
				.put(SUGARCRM_FAX_WORK_FIELD).put(SUGARCRM_STREET_FIELD).put(SUGARCRM_CITY_FIELD)
				.put(SUGARCRM_STATE_FIELD).put(SUGARCRM_POSTAL_CODE_FIELD).put(SUGARCRM_COUNTRY_FIELD)
				.put(SUGARCRM_DATE_MODIFIED_FIELD);
		String sugar_query = SUGAR_CONTACTS_QUERY;
		if (date != null)
			sugar_query = "(contacts.date_modified >= '" + date + "')";
		jso_array.put(token).put(SUGAR_MODULE_CONTACTS).put(sugar_query).put(SUGAR_CONTACTS_ORDER_BY).put(0)
				.put(jso_fields).put(SUGAR_CONTACT_LINK_NAMES).put(1000).put(0);

		final HttpPost post = prepareJSONRequest(jso_array.toString(), GET_METHOD);
		HttpClient httpClient = getConnection();
		Log.i(TAG, "Sending request");
		resp = httpClient.execute(post);
		Log.i(TAG, "Got response");
		if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			if (Log.isLoggable(TAG, Log.VERBOSE)) {
				Log.v(TAG, "Successful authentication");
			}
			Log.i(TAG, "Buffering request");
			BufferedReader rd = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
			List<ISweetContact> contacts = new ArrayList<ISweetContact>();
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}
			rd.close();
			String message = sb.toString();
			JSONArray result;
			JSONObject json = null;
			try {
				Log.i(TAG, "Parsing response");
				json = (JSONObject) new JSONTokener(message).nextValue();
				result = json.getJSONArray("entry_list");
				Log.i(TAG, "Creating contact objects");
				for (int i = 0; i < result.length(); i++) {
					try {
						// ID, first name and last name are compulsory, the rest
						// can be skipped
						JSONObject entrada = result.getJSONObject(i).getJSONObject("name_value_list");
						contacts.add(new SweetContact(entrada.getJSONObject(SUGARCRM_CONTACT_ID_FIELD)
								.getString("value"), entrada.getJSONObject(SUGARCRM_FIRST_NAME_FIELD)
								.getString("value"),
								entrada.getJSONObject(SUGARCRM_LAST_NAME_FIELD).getString("value"),
								getSugarValue(entrada, SUGARCRM_TITLE_FIELD, ""),
								getSugarValue(entrada, SUGARCRM_ACCOUNT_NAME_FIELD, ""),
								getSugarValue(entrada, SUGARCRM_ACCOUNT_ID_FIELD, ""),
								getSugarValue(entrada, SUGARCRM_EMAIL1_FIELD, ""),
								getSugarValue(entrada, SUGARCRM_PHONE_WORK_FIELD, ""),
								getSugarValue(entrada, SUGARCRM_PHONE_MOBILE_FIELD, ""),
								getSugarValue(entrada, SUGARCRM_FAX_WORK_FIELD, ""),
								getSugarValue(entrada, SUGARCRM_STREET_FIELD, ""),
								getSugarValue(entrada, SUGARCRM_CITY_FIELD, ""), 
								getSugarValue(entrada, SUGARCRM_STATE_FIELD, ""),
								getSugarValue(entrada, SUGARCRM_POSTAL_CODE_FIELD, ""),
								getSugarValue(entrada, SUGARCRM_COUNTRY_FIELD, ""),
								getSugarValue(entrada, SUGARCRM_DATE_MODIFIED_FIELD, "")));
					} catch (Exception ex) {
						ex.printStackTrace();
						Log.e(TAG, "Unknown error parsing, skipping entry");
					}
				}

				return contacts;
			} catch (Exception e) {
				if (json != null) {
					Log.i(TAG, "Error parsing json in getNewerContacts. Auth invalid");
					try {

						throw new AuthenticationException(json.getString("description"));
					} catch (JSONException ex) {
						throw new AuthenticationException("Invalid session");
					}
				}
			}
		} else {
			if (Log.isLoggable(TAG, Log.VERBOSE)) {
				Log.v(TAG, "Error authenticating" + resp.getStatusLine());
				throw new AuthenticationException("Invalid session");

			}

		}
		return null;
	}

	@Override
	public List<String> sendNewContacts(String token, List<ISweetContact> contacts, boolean create) throws IOException,
			AuthenticationException {
		final HttpResponse resp;
		ArrayList<String> listaIds = new ArrayList<String>();
		Log.i(TAG, "sendNewContacts()");

		JSONArray jsonData = new JSONArray();
		JSONArray jsonContactList = new JSONArray();
		// TODO: add newer fields (adress and other phones)
		for (ISweetContact c : contacts) {
			JSONArray jsonContactArray = new JSONArray();
			try {
				if (!create)
					setJsonFieldEntry(jsonContactArray, SUGARCRM_CONTACT_ID_FIELD, c.getId());
				setJsonFieldEntry(jsonContactArray, SUGARCRM_FIRST_NAME_FIELD, c.getFirstName());
				setJsonFieldEntry(jsonContactArray, SUGARCRM_LAST_NAME_FIELD, c.getLastName());
				setJsonFieldEntry(jsonContactArray, SUGARCRM_TITLE_FIELD, c.getTitle());
				setJsonFieldEntry(jsonContactArray, SUGARCRM_ACCOUNT_NAME_FIELD, c.getAccountName());
				setJsonFieldEntry(jsonContactArray, SUGARCRM_ACCOUNT_ID_FIELD, c.getAccountId());
				setJsonFieldEntry(jsonContactArray, SUGARCRM_EMAIL1_FIELD, c.getEmail1());
				setJsonFieldEntry(jsonContactArray, SUGARCRM_PHONE_WORK_FIELD, c.getWorkPhone());
				setJsonFieldEntry(jsonContactArray, SUGARCRM_PHONE_MOBILE_FIELD, c.getMobilePhone());
				setJsonFieldEntry(jsonContactArray, SUGARCRM_FAX_WORK_FIELD, c.getWorkFax());
				setJsonFieldEntry(jsonContactArray, SUGARCRM_STREET_FIELD, c.getStreet());
				setJsonFieldEntry(jsonContactArray, SUGARCRM_CITY_FIELD, c.getCity());
				setJsonFieldEntry(jsonContactArray, SUGARCRM_STATE_FIELD, c.getRegion());
				setJsonFieldEntry(jsonContactArray, SUGARCRM_POSTAL_CODE_FIELD, c.getPostalCode());
				setJsonFieldEntry(jsonContactArray, SUGARCRM_COUNTRY_FIELD, c.getCountry());
				setJsonFieldEntry(jsonContactArray, SUGARCRM_DATE_MODIFIED_FIELD, c.getDateModified());
				jsonContactList.put(jsonContactArray);
			} catch (JSONException e) {
				Log.e(TAG, "Error sending contact to the server");
				e.printStackTrace();
			}

		}
		jsonData.put(token).put(SUGAR_MODULE_CONTACTS).put(jsonContactList);

		final HttpPost post = prepareJSONRequest(jsonData.toString(), SET_METHOD);
		HttpClient httpClient = getConnection();
		Log.i(TAG, "Sending request");

		resp = httpClient.execute(post);
		Log.i(TAG, "Got response");
		if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			Log.i(TAG, "Buffering request");
			BufferedReader rd = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}
			rd.close();
			String message = sb.toString();
			Log.i(TAG, "Set entries result: " + message);
			JSONObject response = null;
			try {
				response = (JSONObject) new JSONTokener(message).nextValue();
				JSONArray ids = response.getJSONArray("ids");
				for (int i = 0; i < ids.length(); i++) {
					listaIds.add(ids.getString(i));
				}
			} catch (Exception e) {
				if (response != null) {
					Log.i(TAG, "Error parsing json in sendNewContacts. Auth invalid");
					try {
						throw new AuthenticationException(response.getString("description"));
					} catch (JSONException ex) {
						throw new AuthenticationException("Invalid session");
					}
				}
			}
		}
		return listaIds;
	}

	/**
	 * Prepares a JSON entry from a value and a field Name
	 * 
	 * @param jsonContactArray
	 *            The array to put the entry
	 * @param fieldName
	 *            The SugarCRM field name
	 * @param vale
	 *            The value of the entry
	 * @throws JSONException
	 */
	private void setJsonFieldEntry(JSONArray jsonContactArray, String fieldName, String value) throws JSONException {
		if ((value != null) && (!TextUtils.isEmpty(value.trim()))) {
			jsonContactArray.put(new JSONObject().put("name", fieldName).put("value", value));
		}
	}

	private String getSugarValue(JSONObject json, String key, String d) {
		String val = d;
		try {
			val = json.getJSONObject(key).getString("value");
		} catch (JSONException ex) {
			Log.i(TAG, "Field " + key + " not set in SugarCRM, ignoring.");
		}
		return val;
	}

	/**
	 * Prepares a JSON request encoding the method and the associated JSON data
	 * for a Sugar REST API call and returns an HTTP Post object
	 * 
	 * 
	 * @param rest_data
	 *            The string representation of the JSON encoded request and
	 *            parameters.
	 * @param method
	 *            The Sugar REST API method to call.
	 * @return The HttpPost representing the Sugar REST Api Call
	 * @throws AssertionError
	 */
	private HttpPost prepareJSONRequest(String rest_data, String method) throws AssertionError {
		AbstractHttpEntity entity = null;
		try {
			final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair(KEY_PARAM_METHOD, method));
			params.add(new BasicNameValuePair(KEY_PARAM_INPUT_TYPE, JSON));
			params.add(new BasicNameValuePair(KEY_PARAM_RESPONSE_TYPE, JSON));
			params.add(new BasicNameValuePair("rest_data", rest_data));
			entity = new UrlEncodedFormEntity(params, HTTP.UTF_8);
			final HttpPost post = new HttpPost(mServer);
			post.addHeader(entity.getContentType());
			post.setEntity(entity);
			return post;

		} catch (final UnsupportedEncodingException e) {
			// this should never happen.
			throw new AssertionError(e);
		}
	}

	/**
	 * Get an HttpConnection object. Will create a new one if needed.
	 * 
	 * @return A defualt HTTP connection object
	 */
	private HttpClient getConnection() {
		if (mHttpClient == null) {
			mHttpClient = new DefaultHttpClient();
			final HttpParams params = mHttpClient.getParams();
			HttpConnectionParams.setConnectionTimeout(params, TIMEOUT_OPS);
			HttpConnectionParams.setSoTimeout(params, TIMEOUT_OPS);

			ConnManagerParams.setTimeout(params, TIMEOUT_OPS);

		}
		return mHttpClient;
	}

	/**
	 * Sends the authentication response from server back to the caller main UI
	 * thread through its handler.
	 * 
	 * @param result
	 *            The boolean holding authentication result
	 * @param handler
	 *            The main UI thread's handler instance.
	 * @param context
	 *            The caller Activity's context.
	 * @param message
	 *            SessionID if login was successful or error message if it was
	 *            not.
	 */
	private static void sendResult(final Boolean result, final Handler handler, final Context context,
			final String message) {
		if (handler == null || context == null) {
			return;
		}
		handler.post(new Runnable() {
			public void run() {
				((SweetAuthenticatorActivity) context).onValidationResult(result, message);
			}
		});
	}

	private String encryptor(String password) {
		String pwd = password;

		String temppass = null;

		byte[] defaultBytes = pwd.getBytes();
		try {
			MessageDigest algorithm = MessageDigest.getInstance("MD5");
			algorithm.reset();
			algorithm.update(defaultBytes);
			byte messageDigest[] = algorithm.digest();

			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++) {
				hexString.append(String.format("%02x", 0xFF & messageDigest[i]));
			}
			temppass = hexString.toString();
		} catch (NoSuchAlgorithmException nsae) {
			System.out.println("No Such Algorithm found");
		}

		return temppass;
	}

	@Override
	public void setServer(String server) throws URISyntaxException {
		mServer = new URI(server);
	}

}
