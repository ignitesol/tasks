/**
 * 
 */
package co.usersource.doui.network;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import co.usersource.doui.R;

/**
 * @author rsh This class intended to provide HTTP transport for sync routines.
 *         This class perform authentication against doui GAE service.
 */
public class HttpConnector {

	/** Timeouts for httpClient */
	public static final int REGISTRATION_TIMEOUT = 30 * 1000; // ms
	/** Base URL for DOUI services */
	// TODO replace this with setup one
	public static final String BASE_URL = "http://ec2-54-213-127-94.us-west-2.compute.amazonaws.com";
	
	private DefaultHttpClient httpClient;
	private List<NameValuePair> params;
	private IHttpRequestHandler httpRequestHandler;
	
	/**
	 * Auth routines require to be executed in separate thread. 
	 * This object used as semaphore.
	 */
	private Object authLock = new Object();

	/**
	 * Check whether current instance of the connector has required cookies for
	 * authentication.
	 * 
	 * @return the isAuthenticated
	 */
	public synchronized boolean isAuthenticated() 
	{
		boolean result = false;
		
		for (Cookie cookie : getHttpClient().getCookieStore().getCookies()) 
		{
			Log.i(this.getClass().getName(), "Found cookie: " + cookie.getName());
			
			if (cookie.getName().equals("SACSID") || cookie.getName().equals("ACSID"))
			{
				result = true;
			}
		}
		
		return result;
	}

	/**
	 * Returns HTTP client used for transport routines. Creates new instance if
	 * required.
	 * 
	 * @return HTTP client.
	 */
	private DefaultHttpClient getHttpClient() {
		if (null == httpClient) {
			httpClient = new DefaultHttpClient();
		}
		return httpClient;
	}

	/**
	 * Sends request for given URI with given parameters.
	 * 
	 * @param URI
	 *            - URI for request.
	 * @param params
	 *            - parameters which must send to the server.
	 * 
	 * @param handler
	 *            - handler to be used against response. This handler is
	 *            responsible to parse returned JSON and perform some routines
	 *            on it.
	 * @return - answer from server in JSON format.
	 * @throws IOException
	 * @throws ParseException
	 */
	synchronized public void SendRequest(String method, String URI,
			List<NameValuePair> params, IHttpRequestHandler handler)
			throws ParseException, IOException {
		this.params = params;
		this.httpRequestHandler = handler;
		new PerformRequestTask().execute(method, URI);
	}

	public JSONObject sendRequestMainThread(String URI,
			List<NameValuePair> params) throws ParseException, IOException {
		Log.d(this.getClass().getName(),
				"sendRequestMainThread(" + URI.toString() + ","
						+ params.toString());
		JSONObject result = null;
		try {
			getHttpClient().getParams().setBooleanParameter(
					ClientPNames.HANDLE_REDIRECTS, false);
			final HttpPost postRequest = new HttpPost(URI + "/sync");
			HttpEntity entity = null;

			entity = new UrlEncodedFormEntity(params);
			postRequest.addHeader(entity.getContentType());
			postRequest.setEntity(entity);

			final HttpParams HttpClientParams = getHttpClient().getParams();
			HttpConnectionParams.setConnectionTimeout(HttpClientParams,
					REGISTRATION_TIMEOUT);
			HttpClientParams.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS,
					true);
			HttpConnectionParams.setSoTimeout(HttpClientParams,
					REGISTRATION_TIMEOUT);

			ConnManagerParams
					.setTimeout(HttpClientParams, REGISTRATION_TIMEOUT);
			
			final HttpResponse response = getHttpClient().execute(postRequest);

			final String data = EntityUtils.toString(response.getEntity());
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
			{
				try {
					result = new JSONObject(data);
				} 
				catch (JSONException e) 
				{
					Log.v(this.getClass().getName(), "Cannot parse json from server: " + data.substring(0, 20));
						e.printStackTrace();
				}
			}
			response.getEntity().consumeContent();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			getHttpClient().getParams().setBooleanParameter(
					ClientPNames.HANDLE_REDIRECTS, true);
		}
		return result;
	}

	/**
	 * Performs authentication routines for this connector.
	 * 
	 * @param applicationContext
	 *            context where this connector executed.
	 * @param account
	 *            any account from AccountManager (now only google accounts
	 *            supported).
	 * */
	public boolean authenticate(Context applicationContext, Account account) 
	{
		Log.v(getClass().getName(), "Start authenticate");
		
		try {
			String authToken = AccountManager.get(applicationContext).blockingGetAuthToken(account, "ah", true);
			AccountManager.get(applicationContext).invalidateAuthToken(account.type, authToken);
			authToken = AccountManager.get(applicationContext).blockingGetAuthToken(account, "ah", true);
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(applicationContext);
			String authURL = settings.getString(applicationContext.getString(R.string.prefSyncServerUrl_Key), BASE_URL);
			authURL += "/_ah/login" + "?continue=" + authURL + "/sync&auth=" + authToken;
			
			SendRequest("GET", authURL, null, new IHttpRequestHandler() {
				public void onRequest(HttpResponse response) {
					synchronized (HttpConnector.this.authLock) {
						HttpConnector.this.authLock.notifyAll();
					}
				}
			});
			
			synchronized (HttpConnector.this.authLock) {
				HttpConnector.this.authLock.wait();
			}
		
		} catch (OperationCanceledException e1) {
			e1.printStackTrace();
		} catch (AuthenticatorException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
			
		return isAuthenticated();
	}

	/**
	 * This class intended to be used as separate thread to perform request to
	 * server.
	 * */
	private class PerformRequestTask extends AsyncTask<String, Integer, HttpResponse> 
	{
		protected HttpResponse doInBackground(String... arg) 
		{
			HttpResponse result = null;
			try {
				getHttpClient().getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
				final HttpParams HttpClientParams = getHttpClient().getParams();
				HttpConnectionParams.setConnectionTimeout(HttpClientParams,	REGISTRATION_TIMEOUT);
				HttpConnectionParams.setSoTimeout(HttpClientParams, REGISTRATION_TIMEOUT);
				ConnManagerParams.setTimeout(HttpClientParams, REGISTRATION_TIMEOUT);
				
				if(arg[0] == "GET")
				{
					final HttpGet request = new HttpGet(arg[1]);
					result = getHttpClient().execute(request);
				}
				else
				{
					final HttpPost postRequest = new HttpPost(arg[1]);
					if( params != null )
					{
						HttpEntity entity = null;
						entity = new UrlEncodedFormEntity(params);
						postRequest.addHeader(entity.getContentType());
						postRequest.setEntity(entity);
					}
					result = getHttpClient().execute(postRequest);
				}
				
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				getHttpClient().getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
			}
			return result;
		}

		protected void onPostExecute(HttpResponse result)
		{
			if (httpRequestHandler != null) {
				httpRequestHandler.onRequest(result);
			}
		}
	}
}
