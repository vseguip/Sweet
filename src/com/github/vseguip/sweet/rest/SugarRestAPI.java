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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.github.vseguip.sweet.SweetAuthenticatorActivity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

/*Imeplementa un API para acceder a SugarCRM
 * 
 * Basado en el ejemplo de SampleSyncAdapter.
 * 
 * */
public class SugarRestAPI implements SugarAPI {
	private static final String JSON = "JSON";
	private static final String TAG = "SugarRestAPI";
	private static final String LOGIN_METHOD = "login";
	public static final int TIMEOUT_OPS = 30 * 1000; // ms
	private static URI mServer;

	public SugarRestAPI(String server) throws URISyntaxException {
		setServer(server);
	}

	public boolean validate(String username, String passwd, Context context, Handler handler) {
		return getToken(username, passwd, context, handler) == null;
	}

	public String getToken(String username, String passwd, Context context, Handler handler) {
		final HttpResponse resp;
		HttpEntity entity = null;
		try {
			final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("method", LOGIN_METHOD));
			params.add(new BasicNameValuePair("input_type", JSON));
			params.add(new BasicNameValuePair("response_type", JSON));

			JSONObject jso_user = new JSONObject();
			jso_user.put("user_name", username).put("password", encryptor(passwd));
			JSONObject jso_content = new JSONObject();
			jso_content.put("user_auth", jso_user);
			jso_content.put("application", "Sweet");
			params.add(new BasicNameValuePair("rest_data", jso_content.toString()));
			entity = new UrlEncodedFormEntity(params);

		} catch (final UnsupportedEncodingException e) {
			// this should never happen.
			throw new AssertionError(e);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		final HttpPost post = new HttpPost(mServer);
		post.addHeader(entity.getContentType());
		post.setEntity(entity);

		HttpClient mHttpClient = new DefaultHttpClient();
		final HttpParams params = mHttpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(params, TIMEOUT_OPS);
		HttpConnectionParams.setSoTimeout(params, TIMEOUT_OPS);
		ConnManagerParams.setTimeout(params, TIMEOUT_OPS);
		try {
			resp = mHttpClient.execute(post);
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
			sendResult(false, handler, context,
					"Error trying to validate your credentials. Check you server name and net connectivity.");
			return null;
		} finally {
			if (Log.isLoggable(TAG, Log.VERBOSE)) {
				Log.v(TAG, "getAuthtoken completing");
			}
		}

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
