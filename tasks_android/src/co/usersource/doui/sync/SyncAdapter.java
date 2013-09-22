package co.usersource.doui.sync;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.SyncResult;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import co.usersource.doui.Tasks;
import co.usersource.doui.DouiContentProvider;
import co.usersource.doui.R;
import co.usersource.doui.network.HttpConnector;
import co.usersource.doui.network.IHttpConnectorAuthHandler;

/**
 * This class implements synchronization with server.
 * 
 * @author Sergey Gadzhilov
 * 
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter implements
		OnSharedPreferenceChangeListener {

	/**
	 * The name of the POST request parameter where stored JSON string with data
	 * to be proceed by client or server.
	 * */
	public static final String JSON_REQUEST_PARAM_NAME = "jsonData";

	/**
	 * This is a string to filter only Google accounts to be used on sync
	 * process.
	 * */
	public static final String SYNC_ACCOUNT_TYPE = "com.google";

	/**
	 * The frequency of sync in seconds
	 */
	public static final int SYNC_PERIOD = 300;

	private static final String TAG = "DouiSyncAdapter";

	/** Member for HTTP transport */
	private HttpConnector httpConnector;

	private String prefSyncUrl; // Url where Sync service running
	private Account prefSyncAccount; // Account to be used for sync
	private JsonDataExchangeAdapter jsonDataExchangeAdapter;
	
	public static final String ACTION_SYNC_FINISHED = "co.usersourse.doui.sync_finished";
	

	/**
	 * Auth routines require to be executed in separate thread. This object used
	 * as semaphore.
	 */
	private Object authLock = new Object();

	/**
	 * {@inheritDoc}
	 */
	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		
		jsonDataExchangeAdapter = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPerformSync(final Account account, final Bundle extras,
			final String authority, ContentProviderClient provider,
			final SyncResult syncResult) {

		Log.d(TAG, "onPerformSync");
		this.loadPreferences();
		ContentResolver.addPeriodicSync(account, authority, new Bundle(), SyncAdapter.SYNC_PERIOD);
		
		
		
		
		getHttpConnector().setHttpConnectorAuthHandler(
				new IHttpConnectorAuthHandler() {

					public void onAuthSuccess() {
						ContentResolver.requestSync(account, authority, new Bundle());
						synchronized (SyncAdapter.this.authLock) {
							SyncAdapter.this.authLock.notifyAll();
						}
					}

					public void onAuthFail() {
						Toast.makeText(getContext(),
								"Auth to sync service failed",
								Toast.LENGTH_LONG).show();
						syncResult.stats.numAuthExceptions++;
						synchronized (SyncAdapter.this.authLock) {
							SyncAdapter.this.authLock.notifyAll();
						}
					}
				});
		
		if (getHttpConnector().isAuthenticated()) {
			Log.d(TAG, "httpConnector.isAuthenticated()==true. Perform sync.");
			performSyncRoutines(syncResult);
		} else {
			Log.d(TAG, "httpConnector.isAuthenticated()==false. Perform auth.");
			getHttpConnector().authenticate(getContext(), account);
			try {
				synchronized (SyncAdapter.this.authLock) {
					SyncAdapter.this.authLock.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// TODO Auto-generated method stub

	}

	/**
	 * Main flow of the sync procedure. Loads data from local database. If any
	 * items exists, request new GAE keys for this items. After this send
	 * objects changed after last update and receive objects from GAE.
	 * 
	 * @param syncResult
	 *            SyncAdapter object which contain information about errors.
	 */
	private void performSyncRoutines(final SyncResult syncResult) {
		Log.v(TAG, "Start synchronization (performSyncRoutines)");
		if(jsonDataExchangeAdapter == null){
			jsonDataExchangeAdapter = new JsonDataExchangeAdapter(getContext());
		}
		jsonDataExchangeAdapter.readDataFromLocalDatabase();
		this.updateKeysForNewRecords(jsonDataExchangeAdapter, syncResult);
		this.requestSyncLocalRemote(jsonDataExchangeAdapter, syncResult);
		getContext().sendBroadcast(new Intent(SyncAdapter.ACTION_SYNC_FINISHED));
	}

	/**
	 * Ask GAE for keys for new objects and apply them to new records in the
	 * local database.
	 * 
	 * @param jsonDataExchangeAdapter
	 *            adapter used to store and receive data in local database in
	 *            JSON format.
	 * @param syncResult
	 *            SyncAdapter object which contain information about errors.
	 */
	private void updateKeysForNewRecords(
			JsonDataExchangeAdapter jsonDataExchangeAdapter,
			SyncResult syncResult) {
		if (jsonDataExchangeAdapter.getNewRecords().length() > 0) {
			JSONObject keysForNewRecords = this.requestKeysForNewRecords(
					jsonDataExchangeAdapter, syncResult);
			if (null != keysForNewRecords) {
				try {
					jsonDataExchangeAdapter.updateKeys(keysForNewRecords,
							syncResult);
				} catch (JSONException e) {
					Tasks.placeNotification(getContext(),
							Tasks.SYNC_NOTIFICATION_ID,
							"Unable to parce received key values");
					syncResult.stats.numParseExceptions++;
					e.printStackTrace();
				}
			} else {
				Tasks.placeNotification(getContext(),
						Tasks.SYNC_NOTIFICATION_ID,
						"No keys for received for "
								+ jsonDataExchangeAdapter.getNewRecords()
										.length() + " objects.");
				syncResult.stats.numParseExceptions++;
			}
		}

	}

	/**
	 * Perform HTTP request to GAE for keys for new objects in local database. * @param
	 * jsonDataExchangeAdapter adapter used to store and receive data in local
	 * database in JSON format.
	 * 
	 * @param syncResult
	 *            SyncAdapter object which contain information about errors.
	 * @return JSON object with new key values, which could be used for local
	 *         storage or null on error.
	 */
	private JSONObject requestKeysForNewRecords(
			JsonDataExchangeAdapter jsonDataExchangeAdapter,
			SyncResult syncResult) {
		JSONObject response = null;
		final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(SyncAdapter.JSON_REQUEST_PARAM_NAME,
				jsonDataExchangeAdapter.getNewRecords().toString()));
		try {
			response = getHttpConnector().sendRequestMainThread(prefSyncUrl,
					params);
		} catch (ParseException e1) {
			Log.d(TAG, "Parce error for new keys response");
			syncResult.stats.numParseExceptions++;
			e1.printStackTrace();
		} catch (IOException e1) {
			Log.d(TAG, "I\\O error for new keys response");
			syncResult.stats.numIoExceptions++;
			e1.printStackTrace();
		}
		return response;
	}

	/**
	 * Perform synchronization between local items and remote. All data sent in
	 * this method must have GAE keys. GAE updates records with existent keys
	 * and inserts new.
	 * 
	 * @param jsonDataExchangeAdapter
	 *            adapter used to store and receive data in local database in
	 *            JSON format.
	 * 
	 * @param syncResult
	 *            SyncAdapter object which contain information about errors.
	 */
	private void requestSyncLocalRemote(
			JsonDataExchangeAdapter jsonDataExchangeAdapter,
			SyncResult syncResult) {
		final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(SyncAdapter.JSON_REQUEST_PARAM_NAME,
				jsonDataExchangeAdapter.getLocalData().toString()));
		JSONObject response = null;
		try {
			response = getHttpConnector().sendRequestMainThread(prefSyncUrl,
					params);
		} catch (ParseException e1) {
			Log.d(TAG, "Parce error for data receved from server");
			syncResult.stats.numParseExceptions++;
			e1.printStackTrace();
		} catch (IOException e1) {
			Log.d(TAG, "I\\O error for data receved from server");
			syncResult.stats.numIoExceptions++;
			e1.printStackTrace();
		}
		jsonDataExchangeAdapter.updateLocalDatabase(response);

	}

	/**
	 * This procedure used to load preferences for sync adapter defined by user.
	 */
	private void loadPreferences() {
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(this.getContext());
		sharedPref
				.getBoolean(
						this.getContext()
								.getString(R.string.prefIsSyncable_Key), false);
		prefSyncUrl = sharedPref.getString(
						this.getContext().getString(
								R.string.prefSyncServerUrl_Key),
						this.getContext().getString(
								R.string.prefSyncServerUrl_Default));
		String strPrefSyncTimeframe = sharedPref.getString(this.getContext()
				.getString(R.string.prefSyncRepeatTime_Key), "" + SYNC_PERIOD);
		Integer.parseInt(strPrefSyncTimeframe);
		String strPrefSyncAccount = sharedPref.getString(this.getContext()
				.getString(R.string.prefSyncAccount_Key), "");
		prefSyncAccount = SyncAdapter.getAccountByString(strPrefSyncAccount,
				getContext());
		
		if (null == prefSyncAccount) {
			Log.e(this.getClass().getName(),
					"Wrong account provided in preferences: "
							+ strPrefSyncAccount);
			Tasks
					.placeNotification(
							getContext(),
							Tasks.SYNC_NOTIFICATION_ID,
							"No account available for sync.\nPlease create google account to be able perform sync.");
		}
	}

	/**
	 * Get system account object by it's string representation.
	 * 
	 * @param accountName
	 *            string that identifies account.
	 * @return account object if exists null otherwise
	 */
	private static Account getAccountByString(String accountName,
			Context context) {
		Account result = null;
		Account[] accounts = AccountManager.get(context).getAccounts();
		if (!accountName.equals("")) {
			for (Account account : accounts) {
				if (account.name.equals(accountName)) {
					result = account;
					break;
				}
			}
		} else {
			if (accounts.length > 0) {
				result = accounts[0];
			}
		}
		return result;
	}

	/**
	 * Getter for HTTP connector, creates new if required.
	 * 
	 * @return the httpConnector
	 */
	private HttpConnector getHttpConnector() {
		if (httpConnector == null) {
			httpConnector = new HttpConnector();
		}
		return httpConnector;
	}

	/**
	 * Class method to request sync for Doui GAE service. This method allow to
	 * select appropriate account or raise notification if no available.
	 * 
	 * @param context
	 *            context used to perform routines.
	 */
	public static void requestSync(Context context) {
		
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(context);
		
		if(!sharedPref.getBoolean(context.getString(R.string.prefIsSyncable_Key), false)
				|| !isConnectionOn(context))
		{
			context.sendBroadcast(new Intent(SyncAdapter.ACTION_SYNC_FINISHED));
			return;
		}
		
		String strPrefSyncAccount = sharedPref.getString(context.getString(R.string.prefSyncAccount_Key), "");
		Account prefSyncAccount = SyncAdapter.getAccountByString(strPrefSyncAccount, context);
		
		if (null == prefSyncAccount) {
			Log.e(TAG, "Wrong account provided in preferences: "
					+ strPrefSyncAccount);
			Tasks
					.placeNotification(
							context,
							Tasks.SYNC_NOTIFICATION_ID,
							"No account available for sync. PLease create google account to be able perform sync.");
		} else {
			ContentResolver.requestSync(prefSyncAccount,
					DouiContentProvider.AUTHORITY, new Bundle());
			context.sendBroadcast(new Intent(SyncAdapter.ACTION_SYNC_FINISHED));
		}
	}
	
	/**
	 * Class method to check that Internet connection exists. 
	 * @param context used to get CONNECTIVITY_SERVICE service info.
	 * @return true if connection exists else false.
	 */
	public static boolean isConnectionOn(Context context) {
	    ConnectivityManager cm =
	        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

	    return cm.getActiveNetworkInfo() != null && 
	       cm.getActiveNetworkInfo().isConnectedOrConnecting();
	}
}
