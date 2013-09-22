/**
 * 
 */
package co.usersource.doui.sync;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import co.usersource.doui.DouiContentProvider;
import co.usersource.doui.database.adapters.TableTodoCategoriesAdapter;
import co.usersource.doui.database.adapters.TableTodoItemsAdapter;
import co.usersource.doui.database.adapters.TableTodoStatusAdapter;

/**
 * This class intended to perform exchange data between SQLite database and any
 * other transport using JSON format.
 * 
 * @author rsh
 * 
 */
public class JsonDataExchangeAdapter {

	public static final String JSON_UPDATED_OBJECT_VALUES = "updateObjectValues";
	public static final String JSON_UPDATED_OBJECT_KEY = "dev_updateObjectKey";
	public static final String JSON_UPDATED_OBJECT_TIME = "updateObjectTime";
	public static final String JSON_UPDATED_OBJECT_TYPE = "updateObjectType";
	public static final String JSON_LAST_UPDATE_TIMESTAMP = "lastUpdateTimestamp";
	public static final String JSON_UPDATED_OBJECTS = "updatedObjects";
	public static final String JSON_REQUEST_TYPE = "requestType";
	public static final String JSON_ITEMS_KEYS = "itemsKeys";

	public static final String JSON_UPDATED_TYPE_STATUS = "DouiTodoStatus";
	public static final String JSON_UPDATED_TYPE_CATEGORIES = "DouiTodoCategories";
	public static final String JSON_UPDATED_TYPE_ITEMS = "DouiTodoItem";

	public static final String JSON_REQUEST_TYPE_GEN_KEYS = "generateKeys";
	public static final String JSON_REQUEST_TYPE_UPDATE_DATA = "updateData";

	private static final String TAG = "JsonDataExchangeAdapter";

	private JSONObject localData;

	/**
	 * @return the localData
	 */
	public JSONObject getLocalData() {
		return localData;
	}

	private JSONObject newRecords;

	/**
	 * @return the newRecords
	 */
	public JSONObject getNewRecords() {
		return newRecords;
	}

	private Context context;
	private String mLastUpdateDate;
	private ContentValues m_valuesForUpdate;

	/**
	 * @return the context
	 */
	public Context getContext() {
		return context;
	}

	/**
	 * Constructor.
	 * 
	 * @param context
	 *            the context where this adapter is executed.
	 * */
	public JsonDataExchangeAdapter(Context context) {
		this.context = context;
		this.m_valuesForUpdate = new ContentValues();
	}

	public void updateKeys(JSONObject data, SyncResult syncResult)
			throws JSONException {
		JSONArray updatedObjects = data.getJSONArray(JSON_UPDATED_OBJECTS);

		for (int i = 0; i < updatedObjects.length(); ++i) {

			// Update statuses keys
			if (updatedObjects.getJSONObject(i).get(JSON_UPDATED_OBJECT_TYPE)
					.equals(JsonDataExchangeAdapter.JSON_UPDATED_TYPE_STATUS)) {

				this.updateKeysByType(updatedObjects.getJSONObject(i)
						.getJSONArray(JSON_UPDATED_OBJECT_VALUES),
						JsonDataExchangeAdapter.JSON_UPDATED_TYPE_STATUS);
				this.updateLocalStatuses(updatedObjects.getJSONObject(i)
						.getJSONArray(JSON_UPDATED_OBJECT_VALUES));
			}

			// Update categories keys
			if (updatedObjects
					.getJSONObject(i)
					.get(JSON_UPDATED_OBJECT_TYPE)
					.equals(JsonDataExchangeAdapter.JSON_UPDATED_TYPE_CATEGORIES)) {
				this.updateKeysByType(updatedObjects.getJSONObject(i)
						.getJSONArray(JSON_UPDATED_OBJECT_VALUES),
						JsonDataExchangeAdapter.JSON_UPDATED_TYPE_CATEGORIES);
				this.updateLocalCategories(updatedObjects.getJSONObject(i)
						.getJSONArray(JSON_UPDATED_OBJECT_VALUES));
			}

			// Update items keys
			if (updatedObjects.getJSONObject(i).get(JSON_UPDATED_OBJECT_TYPE)
					.equals(JsonDataExchangeAdapter.JSON_UPDATED_TYPE_ITEMS)) {
				JSONArray keys = updatedObjects.getJSONObject(i).getJSONArray(
						JsonDataExchangeAdapter.JSON_ITEMS_KEYS); 

				// local data update
				JSONArray localUpdatedObjects = localData
						.getJSONArray(JSON_UPDATED_OBJECTS);

				int localItem = 0;
				for (; localItem < localUpdatedObjects.length(); ++localItem) {
					if (localUpdatedObjects
							.getJSONObject(localItem)
							.get(JSON_UPDATED_OBJECT_TYPE)
							.equals(JsonDataExchangeAdapter.JSON_UPDATED_TYPE_ITEMS)) {
						break;
					}
				}

				JSONArray localValues = localUpdatedObjects.getJSONObject(
						localItem).getJSONArray(JSON_UPDATED_OBJECT_VALUES);
				for (int item = 0, keyIndex = 0; item < localValues.length(); ++item) {
					if (localValues.getJSONObject(item)
							.getString(JSON_UPDATED_OBJECT_KEY).equals("null")) {
						localValues.getJSONObject(item).put(
								JSON_UPDATED_OBJECT_KEY,
								keys.getString(keyIndex));
						++keyIndex;
					}
				}

				this.updateLocalItems(localValues);
			}
		}
	}

	/**
	 * This method updates key values for localData objects of the given type
	 * inside JSON packet.
	 * 
	 * @param values
	 *            key values for the objects.
	 * @param type
	 *            the identifier of the objects type to be updated.
	 * */
	private void updateKeysByType(JSONArray values, String type)
			throws JSONException {
		JSONArray localUpdatedObjects = localData
				.getJSONArray(JSON_UPDATED_OBJECTS);

		int localItem = 0;
		for (; localItem < localUpdatedObjects.length(); ++localItem) {
			if (localUpdatedObjects.getJSONObject(localItem)
					.get(JSON_UPDATED_OBJECT_TYPE).equals(type)) {
				break;
			}
		}

		for (int item = 0; item < values.length(); ++item) {
			localUpdatedObjects.getJSONObject(localItem)
					.getJSONArray(JSON_UPDATED_OBJECT_VALUES)
					.put(values.getJSONObject(item));
		}
	}

	/**
	 * This function reads information from local database.
	 */
	public void readDataFromLocalDatabase() {
		Cursor answer;
		String selection;
		this.localData = new JSONObject();
		this.newRecords = new JSONObject();
		try {
			this.localData.put(JsonDataExchangeAdapter.JSON_REQUEST_TYPE,
					JsonDataExchangeAdapter.JSON_REQUEST_TYPE_UPDATE_DATA);
			this.localData.put(JsonDataExchangeAdapter.JSON_UPDATED_OBJECTS,
					new JSONArray());
			// TODO Epoch started not an 2000. May be GAE must be changed to
			// proceed null last update time.
			if (mLastUpdateDate == null) {
				this.localData.put(
						JsonDataExchangeAdapter.JSON_LAST_UPDATE_TIMESTAMP,
						"2000-01-01 00:00:00:00");
			} else {
				this.localData.put(
						JsonDataExchangeAdapter.JSON_LAST_UPDATE_TIMESTAMP,
						mLastUpdateDate);
			}
			
			this.newRecords.put(JsonDataExchangeAdapter.JSON_REQUEST_TYPE,
					JsonDataExchangeAdapter.JSON_REQUEST_TYPE_GEN_KEYS);
			this.newRecords.put(JsonDataExchangeAdapter.JSON_UPDATED_OBJECTS,
					new JSONArray());

		} catch (JSONException e1) {
			e1.printStackTrace();
		}

		// Generate update data for statuses
		if (mLastUpdateDate != null) {
			selection = TableTodoStatusAdapter.TABLE_TODO_STATUSES_LAST_UPDATE
					+ " > '" + mLastUpdateDate + "'";
		} else {
			selection = null;
		}
		answer = getContext().getContentResolver().query(
				DouiContentProvider.TODO_STATUSES_URI, null, selection, null,
				null);
		createJSONData(answer, JsonDataExchangeAdapter.JSON_UPDATED_TYPE_STATUS);
		// ///////////////////////////////////////////////////////////////////////////////////////////
		// Generate update data for categories
		if (mLastUpdateDate != null) {
			selection = TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_LAST_UPDATE
					+ " > '" + mLastUpdateDate + "'";
		} else {
			selection = null;
		}
		answer = getContext().getContentResolver().query(
				DouiContentProvider.TODO_CATEGORIES_URI, null, selection, null,
				null);
		createJSONData(answer,
				JsonDataExchangeAdapter.JSON_UPDATED_TYPE_CATEGORIES);
		// ///////////////////////////////////////////////////////////////////////////////////////////
		// Generate update data for items
		if (mLastUpdateDate != null) {
			selection = TableTodoItemsAdapter.TABLE_TODO_ITEMS_LAST_UPDATE
					+ " > '" + mLastUpdateDate + "'";
		} else {
			selection = null;
		}
		answer = getContext().getContentResolver()
				.query(DouiContentProvider.TODO_ITEMS_URI, null, selection,
						null, null);
		createJSONData(answer, JsonDataExchangeAdapter.JSON_UPDATED_TYPE_ITEMS);
		// ///////////////////////////////////////////////////////////////////////////////////////////
	}

	/**
	 * Generate json object from cursor
	 * 
	 * @param data
	 *            - cursor with data
	 * @param type
	 *            - type of object witch should be generated
	 */
	public void createJSONData(Cursor data, String type) {
		JSONObject keys = new JSONObject();
		JSONArray keysItems = new JSONArray();
		JSONObject updateObjectValues = new JSONObject();
		JSONArray updateObjectItems = new JSONArray();
		JSONObject currentObject = new JSONObject();
		int nItemsCount = 0;

		if (data != null) {
			try {
				keys.put(JsonDataExchangeAdapter.JSON_UPDATED_OBJECT_TYPE, type);
				updateObjectValues.put(
						JsonDataExchangeAdapter.JSON_UPDATED_OBJECT_TYPE, type);

				for (boolean flag = data.moveToFirst(); flag; flag = data
						.moveToNext()) {
					currentObject = new JSONObject();

					if (type.equals(JsonDataExchangeAdapter.JSON_UPDATED_TYPE_STATUS)) {
						currentObject
								.put(JsonDataExchangeAdapter.JSON_UPDATED_OBJECT_KEY,
										data.getString(data
												.getColumnIndex(TableTodoStatusAdapter.TABLE_TODO_STATUSES_OBJECT_KEY)) != null ? data.getString(data
												.getColumnIndex(TableTodoStatusAdapter.TABLE_TODO_STATUSES_OBJECT_KEY))
												: JSONObject.NULL);
						currentObject
								.put(JsonDataExchangeAdapter.JSON_LAST_UPDATE_TIMESTAMP,
										data.getString(data
												.getColumnIndex(TableTodoStatusAdapter.TABLE_TODO_STATUSES_LAST_UPDATE)));
						currentObject
								.put(TableTodoStatusAdapter.TABLE_TODO_STATUSES_NAME,
										data.getString(data
												.getColumnIndex(TableTodoStatusAdapter.TABLE_TODO_STATUSES_NAME)));
						currentObject
								.put("client" // TODO make this constant
										+ TableTodoStatusAdapter.TABLE_TODO_STATUSES_ID,
										data.getString(data
												.getColumnIndex(TableTodoStatusAdapter.TABLE_TODO_STATUSES_ID)));
					}

					if (type.equals(JsonDataExchangeAdapter.JSON_UPDATED_TYPE_CATEGORIES)) {
						currentObject
								.put(JsonDataExchangeAdapter.JSON_UPDATED_OBJECT_KEY,
										data.getString(data
												.getColumnIndex(TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_OBJECT_KEY)) != null ? data.getString(data
												.getColumnIndex(TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_OBJECT_KEY))
												: JSONObject.NULL);
						currentObject
								.put(JsonDataExchangeAdapter.JSON_LAST_UPDATE_TIMESTAMP,
										data.getString(data
												.getColumnIndex(TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_LAST_UPDATE)));
						currentObject
								.put(TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_NAME,
										data.getString(data
												.getColumnIndex(TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_NAME)));
						currentObject
								.put("client"
										+ TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_ID,
										data.getString(data
												.getColumnIndex(TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_ID)));
						currentObject
								.put(TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_IS_DELETED,
										data.getString(data
												.getColumnIndex(TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_IS_DELETED)));
					}

					if (type.equals(JsonDataExchangeAdapter.JSON_UPDATED_TYPE_ITEMS)) {
						currentObject
								.put(JsonDataExchangeAdapter.JSON_UPDATED_OBJECT_KEY,
										data.getString(data
												.getColumnIndex(TableTodoItemsAdapter.TABLE_TODO_ITEMS_OBJECT_KEY)) != null ? data.getString(data
												.getColumnIndex(TableTodoItemsAdapter.TABLE_TODO_ITEMS_OBJECT_KEY))
												: JSONObject.NULL);
						currentObject
								.put(JsonDataExchangeAdapter.JSON_LAST_UPDATE_TIMESTAMP,
										data.getString(data
												.getColumnIndex(TableTodoItemsAdapter.TABLE_TODO_ITEMS_LAST_UPDATE)));
						currentObject
								.put("client"
										+ TableTodoItemsAdapter.TABLE_TODO_ITEMS_ID,
										data.getString(data
												.getColumnIndex(TableTodoItemsAdapter.TABLE_TODO_ITEMS_ID)));
						currentObject
								.put(TableTodoItemsAdapter.TABLE_TODO_ITEMS_BODY,
										data.getString(data
												.getColumnIndex(TableTodoItemsAdapter.TABLE_TODO_ITEMS_BODY)));
						currentObject
								.put(TableTodoItemsAdapter.TABLE_TODO_ITEMS_TITLE,
										data.getString(data
												.getColumnIndex(TableTodoItemsAdapter.TABLE_TODO_ITEMS_TITLE)));

						if (data.getString(data
								.getColumnIndex(TableTodoItemsAdapter.TABLE_TODO_ITEMS_FK_CATEGORY)) == null) {
							currentObject
									.put(TableTodoItemsAdapter.TABLE_TODO_ITEMS_FK_CATEGORY,
											JSONObject.NULL);
						} else {
							String categotyObjectKey = getCategoryObjectKey(data
									.getInt(data
											.getColumnIndex(TableTodoItemsAdapter.TABLE_TODO_ITEMS_FK_CATEGORY)));

							if (categotyObjectKey != null
									&& !categotyObjectKey.isEmpty()) {
								currentObject
										.put(TableTodoItemsAdapter.TABLE_TODO_ITEMS_FK_CATEGORY,
												categotyObjectKey);
							} else {
								currentObject
										.put(TableTodoItemsAdapter.TABLE_TODO_ITEMS_FK_CATEGORY,
												data.getString(data
														.getColumnIndex(TableTodoItemsAdapter.TABLE_TODO_ITEMS_FK_CATEGORY)));
							}

						}

						if (data.getString(data
								.getColumnIndex(TableTodoItemsAdapter.TABLE_TODO_ITEMS_FK_STATUS)) == null) {
							currentObject
									.put(TableTodoItemsAdapter.TABLE_TODO_ITEMS_FK_STATUS,
											JSONObject.NULL);
						} else {
							String statusObjectKey = getStatusObjectKey(data
									.getInt(data
											.getColumnIndex(TableTodoItemsAdapter.TABLE_TODO_ITEMS_FK_STATUS)));

							if (statusObjectKey != null
									&& !statusObjectKey.isEmpty()) {
								currentObject
										.put(TableTodoItemsAdapter.TABLE_TODO_ITEMS_FK_STATUS,
												statusObjectKey);
							} else {
								currentObject
										.put(TableTodoItemsAdapter.TABLE_TODO_ITEMS_FK_STATUS,
												data.getString(data
														.getColumnIndex(TableTodoItemsAdapter.TABLE_TODO_ITEMS_FK_STATUS)));
							}

						}
					}

					if (currentObject.getString(
							JsonDataExchangeAdapter.JSON_UPDATED_OBJECT_KEY)
							.equals("null")) {
						if (type == JsonDataExchangeAdapter.JSON_UPDATED_TYPE_ITEMS) {
							++nItemsCount;
							updateObjectItems.put(currentObject);
						} else {
							keysItems.put(currentObject);
						}
					} else {
						updateObjectItems.put(currentObject);
					}
				}

				updateObjectValues.put(
						JsonDataExchangeAdapter.JSON_UPDATED_OBJECT_VALUES,
						updateObjectItems);
				this.localData.getJSONArray(
						JsonDataExchangeAdapter.JSON_UPDATED_OBJECTS).put(
						updateObjectValues);

				if (type == JsonDataExchangeAdapter.JSON_UPDATED_TYPE_ITEMS) {
					keys.put("itemsCount", nItemsCount);
				} else {
					keys.put(
							JsonDataExchangeAdapter.JSON_UPDATED_OBJECT_VALUES,
							keysItems);
				}
				this.newRecords.getJSONArray(
						JsonDataExchangeAdapter.JSON_UPDATED_OBJECTS).put(keys);

			} catch (JSONException e) {
				Log.v(TAG, "createJSONDataForServer failed");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Update local database with data from server
	 * 
	 * @param data
	 *            - json object with data from server
	 */
	public void updateLocalDatabase(JSONObject data) {

		if (data != null) {

			try {
				
				JSONArray dataFromServer = data
						.getJSONArray(JSON_UPDATED_OBJECTS);

				JSONArray statuses = new JSONArray();
				JSONArray categories = new JSONArray();
				JSONArray items = new JSONArray();
				for (int i = 0; i < dataFromServer.length(); ++i) {
					if (dataFromServer
							.getJSONObject(i)
							.get(JSON_UPDATED_OBJECT_TYPE)
							.equals(JsonDataExchangeAdapter.JSON_UPDATED_TYPE_STATUS)) {
						statuses = dataFromServer.getJSONObject(i)
								.getJSONArray(JSON_UPDATED_OBJECT_VALUES);
						continue;
					}

					if (dataFromServer
							.getJSONObject(i)
							.get(JSON_UPDATED_OBJECT_TYPE)
							.equals(JsonDataExchangeAdapter.JSON_UPDATED_TYPE_CATEGORIES)) {
						categories = dataFromServer.getJSONObject(i)
								.getJSONArray(JSON_UPDATED_OBJECT_VALUES);
						continue;
					}

					if (dataFromServer
							.getJSONObject(i)
							.get(JSON_UPDATED_OBJECT_TYPE)
							.equals(JsonDataExchangeAdapter.JSON_UPDATED_TYPE_ITEMS)) {
						items = dataFromServer.getJSONObject(i).getJSONArray(
								JSON_UPDATED_OBJECT_VALUES);
						continue;
					}
				}

				this.updateLocalStatuses(statuses);
				this.updateLocalCategories(categories);
				this.updateLocalItems(items);
				this.cleanDeletedCategories();
				
				mLastUpdateDate = data
						.getString(JsonDataExchangeAdapter.JSON_LAST_UPDATE_TIMESTAMP);

			} catch (JSONException e) {
				Log.v(TAG, "Data from server is not valid!!");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Add field to set for update local database
	 * 
	 * @param fieldName
	 *            - name of field in db
	 * @param value
	 *            - JSON object with values
	 * @param valueName
	 *            - value name in json object. If null then fieldName will be
	 *            use as name in json object
	 */
	private void addFieldToUpdate(String fieldName, JSONObject value,
			String valueName) {
		String data;
		try {
			if (valueName != null) {
				data = value.getString(valueName);
			} else {
				data = value.getString(fieldName);
			}
			m_valuesForUpdate.put(fieldName, data);
		} catch (JSONException e) {
			Log.v(TAG, "Cannot add value for field " + fieldName);
			e.printStackTrace();
		}

	}

	/**
	 * This method gets status object key by his ID
	 * 
	 * @param nStatusID
	 *            status ID
	 * @return status object key or empty string if status not found
	 */
	private String getStatusObjectKey(int nStatusID) {
		String strResult = "";
		Cursor data;
		Uri uri = Uri.parse(DouiContentProvider.TODO_STATUSES_URI.toString()
				+ "/" + nStatusID);
		data = getContext().getContentResolver().query(uri, null, null, null,
				null);
		if (data != null && data.moveToFirst()) {
			strResult = data
					.getString(data
							.getColumnIndex(TableTodoStatusAdapter.TABLE_TODO_STATUSES_OBJECT_KEY));
		}
		return strResult;

	}

	/**
	 * This method gets category object key by his ID
	 * 
	 * @param nCategoryID
	 *            category ID
	 * @return category object key or empty string if category not found.
	 */
	private String getCategoryObjectKey(int nCategoryID) {
		String strResult = "";
		Cursor data;
		Uri uri = Uri.parse(DouiContentProvider.TODO_CATEGORIES_URI.toString()
				+ "/" + nCategoryID);
		data = getContext().getContentResolver().query(uri, null, null, null,
				null);
		if (data != null && data.moveToFirst()) {
			strResult = data
					.getString(data
							.getColumnIndex(TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_OBJECT_KEY));
		}
		return strResult;
	}

	/**
	 * This method gets category id by his object key
	 * 
	 * @param strObjectKey
	 *            category object key
	 * @return category ID or -1 if category not found
	 */
	private int getCategoryIDByObjectKey(String strObjectKey) {
		int nResult = -1;
		Cursor data;
		String strSelection = TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_OBJECT_KEY
				+ " = '" + strObjectKey + "'";
		data = getContext().getContentResolver().query(
				DouiContentProvider.TODO_CATEGORIES_URI, null, strSelection,
				null, null);
		if (data != null && data.moveToFirst()) {
			nResult = data
					.getInt(data
							.getColumnIndex(TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_ID));
		}
		return nResult;
	}

	/**
	 * This method gets status id by his object_key
	 * 
	 * @param strObjectKey
	 *            status object key
	 * @return status id or -1 if status not found
	 */
	private int getStatusIDByObjectKey(String strObjectKey) {
		int nResult = -1;
		Cursor data;
		String strSelection = TableTodoStatusAdapter.TABLE_TODO_STATUSES_OBJECT_KEY
				+ " = '" + strObjectKey + "'";
		data = getContext().getContentResolver().query(
				DouiContentProvider.TODO_STATUSES_URI, null, strSelection,
				null, null);
		if (data != null && data.moveToFirst()) {
			nResult = data
					.getInt(data
							.getColumnIndex(TableTodoStatusAdapter.TABLE_TODO_STATUSES_ID));
		}
		return nResult;
	}

	/**
	 * This method cleans categories marked as deleted
	 */
	private void cleanDeletedCategories() {
		String where = TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_IS_DELETED
				+ " = 1";
		getContext().getContentResolver().delete(
				DouiContentProvider.TODO_CATEGORIES_URI, where, null);
	}

	private void updateLocalStatuses(JSONArray data) {
		Uri uriForUpdate;
		m_valuesForUpdate.clear();
		for (int i = 0; i < data.length(); ++i) {
			try {
				addFieldToUpdate(
						TableTodoStatusAdapter.TABLE_TODO_STATUSES_OBJECT_KEY,
						data.getJSONObject(i), JSON_UPDATED_OBJECT_KEY);
				addFieldToUpdate(
						TableTodoStatusAdapter.TABLE_TODO_STATUSES_NAME,
						data.getJSONObject(i), null);

				if (!data.getJSONObject(i).getString("client_id")
						.equals("null")) {

					uriForUpdate = Uri
							.parse(DouiContentProvider.TODO_STATUSES_URI
									.toString()
									+ "/"
									+ data.getJSONObject(i).getString(
											"client_id"));

					getContext().getContentResolver().update(uriForUpdate,
							m_valuesForUpdate, null, null);
				}

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private void updateLocalCategories(JSONArray data) {
		String selection;
		Cursor localData;
		Uri uriForUpdate;

		m_valuesForUpdate.clear();
		for (int i = 0; i < data.length(); ++i) {
			try {
				addFieldToUpdate(
						TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_IS_DELETED,
						data.getJSONObject(i), null);
				addFieldToUpdate(
						TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_OBJECT_KEY,
						data.getJSONObject(i), JSON_UPDATED_OBJECT_KEY);
				addFieldToUpdate(
						TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_NAME,
						data.getJSONObject(i), null);

				if (data.getJSONObject(i).getString("client_id").equals("null")) {

					selection = TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_OBJECT_KEY
							+ " = '"
							+ data.getJSONObject(i).getString(
									JSON_UPDATED_OBJECT_KEY) + "'";

					localData = getContext().getContentResolver().query(
							DouiContentProvider.TODO_CATEGORIES_URI, null,
							selection, null, null);

					if (localData != null && localData.moveToFirst()) {

						uriForUpdate = Uri
								.parse(DouiContentProvider.TODO_CATEGORIES_URI
										.toString()
										+ "/"
										+ localData.getInt(localData
												.getColumnIndex(TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_ID)));

						getContext().getContentResolver().update(uriForUpdate,
								m_valuesForUpdate, null, null);

					} else {
						if (getContext().getContentResolver().insert(
								DouiContentProvider.TODO_CATEGORIES_URI,
								m_valuesForUpdate) == null) {

							Log.v(TAG,
									"Cannot insert new item for "
											+ DouiContentProvider.TODO_CATEGORIES_URI
													.toString());
						}
					}

				} else {
					uriForUpdate = Uri
							.parse(DouiContentProvider.TODO_CATEGORIES_URI
									.toString()
									+ "/"
									+ data.getJSONObject(i).getString(
											"client_id"));
					getContext().getContentResolver().update(uriForUpdate,
							m_valuesForUpdate, null, null);
				}

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	/**
	 * Store data from JSON array to the local database.
	 * 
	 * @param data
	 *            JSON data to be stored to the database
	 * 
	 */
	private void updateLocalItems(JSONArray data) {
		String selection;
		Cursor localData;
		Uri uriForUpdate;
		m_valuesForUpdate.clear();
		for (int i = 0; i < data.length(); ++i) {
			try {
				this.addFieldToUpdate(TableTodoItemsAdapter.TABLE_TODO_ITEMS_BODY,
						data.getJSONObject(i), null);
				this.addFieldToUpdate(TableTodoItemsAdapter.TABLE_TODO_ITEMS_TITLE,
						data.getJSONObject(i), null);
				this.addFieldToUpdate(
						TableTodoItemsAdapter.TABLE_TODO_ITEMS_OBJECT_KEY,
						data.getJSONObject(i), JSON_UPDATED_OBJECT_KEY);

				// In case if item has category, load it by objectKey.
				if (!data
						.getJSONObject(i)
						.getString(
								TableTodoItemsAdapter.TABLE_TODO_ITEMS_FK_CATEGORY)
						.equals("null")) {

					int nCategoryId = getCategoryIDByObjectKey(data
							.getJSONObject(i)
							.getString(
									TableTodoItemsAdapter.TABLE_TODO_ITEMS_FK_CATEGORY));

					if (nCategoryId != -1) {
						m_valuesForUpdate
								.put(TableTodoItemsAdapter.TABLE_TODO_ITEMS_FK_CATEGORY,
										nCategoryId);
					}
				}

				// In case if item has status, load it by objectKey.
				if (!data
						.getJSONObject(i)
						.getString(
								TableTodoItemsAdapter.TABLE_TODO_ITEMS_FK_STATUS)
						.equals("null")) {

					int nStatusId = getStatusIDByObjectKey(data
							.getJSONObject(i)
							.getString(
									TableTodoItemsAdapter.TABLE_TODO_ITEMS_FK_STATUS));
					if (nStatusId != -1) {
						m_valuesForUpdate
								.put(TableTodoItemsAdapter.TABLE_TODO_ITEMS_FK_STATUS,
										nStatusId);
					}
				}

				// Insert or update item
				if (data.getJSONObject(i).getString("client_id").equals("null")) {
					// This means that no client ID exists in data, so this item was stored not from this device but already exists in GAE.
					selection = TableTodoItemsAdapter.TABLE_TODO_ITEMS_OBJECT_KEY
							+ " = '"
							+ data.getJSONObject(i).getString(
									JSON_UPDATED_OBJECT_KEY) + "'";

					localData = getContext().getContentResolver().query(
							DouiContentProvider.TODO_ITEMS_URI, null,
							selection, null, null);

					if (localData != null && localData.moveToFirst()) {

						uriForUpdate = Uri
								.parse(DouiContentProvider.TODO_ITEMS_URI
										.toString()
										+ "/"
										+ localData.getInt(localData
												.getColumnIndex(TableTodoItemsAdapter.TABLE_TODO_ITEMS_ID)));

						getContext().getContentResolver().update(uriForUpdate,
								m_valuesForUpdate, null, null);

					} else {
						Uri itemUri = Uri
								.parse(DouiContentProvider.TODO_CATEGORIES_URI
										.toString()
										+ "/"
										+ getCategoryIDByObjectKey(data
												.getJSONObject(i)
												.getString(
														TableTodoItemsAdapter.TABLE_TODO_ITEMS_FK_CATEGORY))
										+ "/" + DouiContentProvider.TODO_PATH);

						if (getContext().getContentResolver().insert(itemUri,
								m_valuesForUpdate) == null) {

							Log.v(TAG,
									"Cannot insert new item for "
											+ DouiContentProvider.TODO_ITEMS_URI
													.toString());
						}
					}

				} else {
					uriForUpdate = Uri.parse(DouiContentProvider.TODO_ITEMS_URI
							.toString()
							+ "/"
							+ data.getJSONObject(i).getString("client_id"));

					getContext().getContentResolver().update(uriForUpdate,
							m_valuesForUpdate, null, null);
				}

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

}
