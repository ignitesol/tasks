/**
 * 
 */
package co.usersource.doui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import co.usersource.doui.database.DouiSQLiteOpenHelper;
import co.usersource.doui.database.adapters.TableTodoCategoriesAdapter;
import co.usersource.doui.database.adapters.TableTodoContextsAdapter;
import co.usersource.doui.database.adapters.TableTodoItemsAdapter;
import co.usersource.doui.database.adapters.TableTodoStatusAdapter;

/**
 * @author rsh
 * 
 */
public class DouiContentProvider extends ContentProvider {

	/** Root part of the content provider URI. */
	public static final String AUTHORITY = "co.usersource.doui.contentprovider";

	/** Root URI for this content provider. */
	public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

	/** Suffix used to construct URI to access todo-categories array. */
	public static final String TODO_CATEGORIES_PATH = "todo_categories";
	/** Full URI to access todo-categories array. */
	public static final Uri TODO_CATEGORIES_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + TODO_CATEGORIES_PATH);
	/** Id for uri that match set of categories of TODOs. */
	public static final int TODO_CATEGORIES_URI_ID = 10;
	/** Id for uri that match concrete todo category id from list. */
	public static final int TODO_CATEGORY_URI_ID = 20;
	/**
	 * Id for uri that match concrete todo category and must return a list of
	 * its items.
	 */
	public static final int TODO_CATEGORY_LIST_URI_ID = 21;

	/** Suffix used to construct URI to access contexts list of todos. */
	public static final String TODO_CONTEXTS_PATH = "contexts";
	/** Full URI to access contexts list of todos. */
	public static final Uri TODO_CONTEXTS_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + TODO_CONTEXTS_PATH);
	/** Id for URI match context list. */
	public static final int TODO_CONTEXTS_URI_ID = 30;
	/** Id for URI match concrete context. */
	public static final int TODO_CONTEXT_URI_ID = 31;
	/**
	 * Id for URI that match concrete context. This URI returns list. No sense
	 * to access to single context, as it is does not exists as separate object.
	 */
	public static final int TODO_CONTEXT_LIST_URI_ID = 40;

	/** Suffix used to construct URI to access concrete todo item. */
	public static final String TODO_PATH = "todo";
	/** Full URI to access concrete todo item. */
	public static final Uri TODO_URI = Uri.parse(TODO_CATEGORIES_URI.toString()
			+ "/#/" + TODO_PATH);
	/** Id for concrete todo item list URI accessed from status path. */
	public static final int TODO_STATUS_ITEM_URI_ID = 60;
	/** Id for concrete todo item list URI accessed from category path. */
	public static final int TODO_CATEGORIES_ITEM_URI_ID = 61;
	/** Id for concrete todo item list URI accessed from category path. */
	public static final int TODO_CONTEXT_ITEM_URI_ID = 62;

	/** Suffix used to construct URI to access item statuses. */
	public static final String TODO_STATUSES_PATH = "statuses";
	/** Full URI to access todo statuses. */
	public static final Uri TODO_STATUSES_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + TODO_STATUSES_PATH);
	/** Id for statuses URI. */
	public static final int TODO_STATUSES_URI_ID = 70;
	/** Id for concrete status URI. */
	public static final int TODO_STATUS_URI_ID = 80;
	/** Id for URI which match a list of items for concrete status. */
	public static final int TODO_STATUS_LIST_URI_ID = 81;
	
	/** Suffix used to construct URI to access todo-items array. */
	public static final String TODO_ITEMS_PATH = "todo_items";
	/** Full URI to access todo-items array. */
	public static final Uri TODO_ITEMS_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + TODO_ITEMS_PATH);
	/** Id for uri that match set of items of TODOs. */
	public static final int TODO_ITEMS_URI_ID = 90;
	/** Id for uri that match set of concrete item of TODOs. */
	public static final int TODO_ITEM_URI_ID = 91;

	/** Member responsible to determinate what kind of the URI passed. */
	public static final UriMatcher sURIMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);
	static {
		sURIMatcher.addURI(AUTHORITY, TODO_CATEGORIES_PATH,
				TODO_CATEGORIES_URI_ID);
		sURIMatcher.addURI(AUTHORITY, TODO_CATEGORIES_PATH + "/#",
				TODO_CATEGORY_URI_ID);

		sURIMatcher.addURI(AUTHORITY, TODO_STATUSES_PATH, TODO_STATUSES_URI_ID);
		sURIMatcher.addURI(AUTHORITY, TODO_STATUSES_PATH + "/#",
				TODO_STATUS_URI_ID);

		sURIMatcher.addURI(AUTHORITY, TODO_CONTEXTS_PATH, TODO_CONTEXTS_URI_ID);
		sURIMatcher.addURI(AUTHORITY, TODO_CONTEXTS_PATH + "/*/", TODO_CONTEXT_URI_ID);

		// Todo lists URIs
		sURIMatcher.addURI(AUTHORITY, TODO_CATEGORIES_PATH + "/#/" + TODO_PATH,
				TODO_CATEGORY_LIST_URI_ID);
		sURIMatcher.addURI(AUTHORITY, TODO_STATUSES_PATH + "/#/" + TODO_PATH,
				TODO_STATUS_LIST_URI_ID);
		sURIMatcher.addURI(AUTHORITY, TODO_CONTEXTS_PATH + "/*/" + TODO_PATH,
				TODO_CONTEXT_LIST_URI_ID);

		sURIMatcher.addURI(AUTHORITY, TODO_CATEGORIES_PATH + "/#/" + TODO_PATH
				+ "/#", TODO_CATEGORIES_ITEM_URI_ID);
		sURIMatcher.addURI(AUTHORITY, TODO_STATUSES_PATH + "/#/" + TODO_PATH
				+ "/#", TODO_STATUS_ITEM_URI_ID);
		sURIMatcher.addURI(AUTHORITY, TODO_CONTEXTS_PATH + "/*/" + TODO_PATH
				+ "/#", TODO_CONTEXT_ITEM_URI_ID);
		
		sURIMatcher.addURI(AUTHORITY, TODO_ITEMS_PATH,
				TODO_ITEMS_URI_ID);
		sURIMatcher.addURI(AUTHORITY, TODO_ITEMS_PATH + "/#",
				TODO_ITEM_URI_ID);
	}

	private DouiSQLiteOpenHelper douiSQLiteOpenHelper;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#delete(android.net.Uri,
	 * java.lang.String, java.lang.String[])
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int result = 0;
		int uriType = sURIMatcher.match(uri);
		switch (uriType) {

		case TODO_CATEGORIES_URI_ID:
			result = douiSQLiteOpenHelper.getTableTodoCategoriesAdapter()
					.delete(selection, selectionArgs);
			break;
		// Single row delete
		case TODO_CATEGORY_URI_ID: {
			String selectConditions = TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_ID
					+ "= ?";
			String selectConditionsArgs[] = { uri.getLastPathSegment() };
			douiSQLiteOpenHelper.getTableTodoCategoriesAdapter().delete(
					selectConditions, selectConditionsArgs);
		}
			break;

		case TODO_CATEGORIES_ITEM_URI_ID:
		case TODO_STATUS_ITEM_URI_ID:
			String itemSelection = TableTodoItemsAdapter.TABLE_TODO_ITEMS_ID
					+ "=?";
			String itemSelectionArgs[] = { uri.getLastPathSegment() };
			result = douiSQLiteOpenHelper.getTableTodoItemsAdapter().delete(
					itemSelection, itemSelectionArgs);
			break;
		default:
			Log.e(this.getClass().getName(),
					"Unknown URI type passed to query(...): " + uriType);
			throw new IllegalArgumentException(
					"Unknown URI type passed to query(...): " + uriType);
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 */
	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#insert(android.net.Uri,
	 * android.content.ContentValues)
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Uri result = null;
		long newItemId = -1;
		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
		case TODO_CATEGORIES_URI_ID:
			newItemId = douiSQLiteOpenHelper.getTableTodoCategoriesAdapter()
					.insert(values);
			if (newItemId > -1) {
				result = Uri.parse(TODO_CATEGORIES_URI.toString() + newItemId);
			}
			break;
		case TODO_CATEGORY_URI_ID:
			Log.e(this.getClass().getName(),
					"Attempt to insert new list from list URI:  " + uri);
			break;
		case TODO_CATEGORY_LIST_URI_ID:
		case TODO_STATUS_LIST_URI_ID:
			newItemId = douiSQLiteOpenHelper.getTableTodoItemsAdapter().insert(
					values);
			if (newItemId > -1) {
				result = Uri.parse(uri.toString() + "/" + newItemId);
			}
			break;

		case TODO_CONTEXT_ITEM_URI_ID:
		case TODO_CATEGORIES_ITEM_URI_ID:
		case TODO_STATUS_ITEM_URI_ID:
			Log.e(this.getClass().getName(),
					"Attempt to insert new todo item from todo item URI:  "
							+ uri);
			break;
		default:
			Log.e(this.getClass().getName(),
					"Unknown URI type passed to query(...): " + uriType);
			throw new IllegalArgumentException(
					"Unknown URI type passed to query(...): " + uriType);
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#onCreate()
	 */
	@Override
	public boolean onCreate() {
		douiSQLiteOpenHelper = new DouiSQLiteOpenHelper(getContext());
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#query(android.net.Uri,
	 * java.lang.String[], java.lang.String, java.lang.String[],
	 * java.lang.String)
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Cursor result = null;
		int uriType = sURIMatcher.match(uri);

		switch (uriType) {
		case TODO_CATEGORIES_URI_ID:
			result = douiSQLiteOpenHelper.getTableTodoCategoriesAdapter()
					.query(projection, selection, selectionArgs, sortOrder);
			break;
		case TODO_CATEGORY_URI_ID: {
			String selectConditions = TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_ID
					+ "= ?";
			String selectConditionsArgs[] = { uri.getLastPathSegment() };
			result = douiSQLiteOpenHelper.getTableTodoCategoriesAdapter()
					.query(projection, selectConditions, selectConditionsArgs,
							sortOrder);
		}
			break;

		case TODO_STATUSES_URI_ID:
			result = douiSQLiteOpenHelper.getTableTodoStatusAdapter().query(
					projection, selection, selectionArgs, sortOrder);
			break;
		case TODO_STATUS_URI_ID: {
			String selectConditions = TableTodoStatusAdapter.TABLE_TODO_STATUSES_ID
					+ "= ?";
			String selectConditionsArgs[] = { uri.getLastPathSegment() };
			result = douiSQLiteOpenHelper.getTableTodoStatusAdapter().query(
					projection, selectConditions, selectConditionsArgs,
					sortOrder);
		}
			break;
		case TODO_CONTEXTS_URI_ID: {
			result = douiSQLiteOpenHelper.getTableTodoContextsAdapter().query(
					projection, selection, selectionArgs, sortOrder);
		}
			break;
		case TODO_CONTEXT_URI_ID: {
			String selectConditions = TableTodoContextsAdapter.TABLE_TODO_CONTEXTS_NAME
					+ "= ?";
			String selectConditionsArgs[] = { uri.getLastPathSegment() };
			result = douiSQLiteOpenHelper.getTableTodoContextsAdapter().query(
					projection, selectConditions, selectConditionsArgs, sortOrder);
		}
			break;
			
		// List queries
		case TODO_STATUS_LIST_URI_ID: {
			List<String> uriSegments = uri.getPathSegments();
			String listId = uriSegments.get((uriSegments.size() - 1) - 1);
			String selectConditions = TableTodoItemsAdapter.TABLE_TODO_ITEMS_FK_STATUS
					+ "= ? ";
			if (null != selection) {
				selectConditions += " and (" + selection + ")";
			}
			List<String> listSelectConditionsArgs = new ArrayList<String>();
			listSelectConditionsArgs.add(listId);
			if (null != selectionArgs) {
				listSelectConditionsArgs.addAll(Arrays.asList(selectionArgs));
			}
			String selectConditionsArgs[] = listSelectConditionsArgs
					.toArray(new String[listSelectConditionsArgs.size()]);
			result = douiSQLiteOpenHelper.getTableTodoItemsAdapter().query(
					projection, selectConditions, selectConditionsArgs,
					sortOrder);
		}
			break;
		case TODO_CATEGORY_LIST_URI_ID: {
			List<String> uriSegments = uri.getPathSegments();
			String listId = uriSegments.get((uriSegments.size() - 1) - 1);
			String selectConditions = TableTodoItemsAdapter.TABLE_TODO_ITEMS_FK_CATEGORY
					+ "= ? ";
			if (null != selection) {
				selectConditions += " and (" + selection + ")";
			}
			List<String> listSelectConditionsArgs = new ArrayList<String>();
			listSelectConditionsArgs.add(listId);
			if (null != selectionArgs) {
				listSelectConditionsArgs.addAll(Arrays.asList(selectionArgs));
			}
			String selectConditionsArgs[] = listSelectConditionsArgs
					.toArray(new String[listSelectConditionsArgs.size()]);
			result = douiSQLiteOpenHelper.getTableTodoItemsAdapter().query(
					projection, selectConditions, selectConditionsArgs,
					sortOrder);
		}
			break;
		case TODO_CONTEXT_LIST_URI_ID: {
			String contextName = uri.getPathSegments().get(uri.getPathSegments().size()-2);
			result = douiSQLiteOpenHelper.getTableTodoContextsAdapter()
					.queryContextItems(contextName); // TODO Make this extendable with additional selection params
		}
			break;

		// Item queries
		case TODO_CONTEXT_ITEM_URI_ID:
		case TODO_CATEGORIES_ITEM_URI_ID:
		case TODO_STATUS_ITEM_URI_ID: {
			String selectConditions = TableTodoItemsAdapter.TABLE_TODO_ITEMS_ID
					+ "= ?";
			String selectConditionsArgs[] = { uri.getLastPathSegment() };
			result = douiSQLiteOpenHelper.getTableTodoItemsAdapter().query(
					projection, selectConditions, selectConditionsArgs,
					sortOrder);
		}
			break;

		//All items query
		case TODO_ITEMS_URI_ID:
			result = douiSQLiteOpenHelper.getTableTodoItemsAdapter().query(
					projection, selection, selectionArgs, sortOrder);
			break;
		default:
			Log.e(this.getClass().getName(),
					"Unknown URI type passed to query(...): " + uriType);
			throw new IllegalArgumentException(
					"Unknown URI type passed to query(...): " + uriType);
		}
		return result;
	}

	/**
	 * Updates values in the database. Uri must be in list context. Selection
	 * should be used to set concrete values.
	 * */
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int result = 0;
		int uriType = sURIMatcher.match(uri);
		switch (uriType) {

		// Batch update of multiple items
		case TODO_CATEGORIES_URI_ID:
			douiSQLiteOpenHelper.getTableTodoCategoriesAdapter().update(values,
					selection, selectionArgs);
			break;
		// Single row update
		case TODO_CATEGORY_URI_ID: {
			String selectConditions = TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_ID
					+ "= ?";
			String selectConditionsArgs[] = { uri.getLastPathSegment() };
			douiSQLiteOpenHelper.getTableTodoCategoriesAdapter().update(values,
					selectConditions, selectConditionsArgs);
		}
			break;
			
		case TODO_STATUS_URI_ID:{
			String selectConditions = TableTodoStatusAdapter.TABLE_TODO_STATUSES_ID
					+ "= ?";
			String selectConditionsArgs[] = { uri.getLastPathSegment() };
			douiSQLiteOpenHelper.getTableTodoStatusAdapter().update(values,
					selectConditions, selectConditionsArgs);
		}
			break;

		case TODO_CONTEXT_ITEM_URI_ID:
		case TODO_CATEGORIES_ITEM_URI_ID:
		case TODO_STATUS_ITEM_URI_ID: {
			String selectConditions = TableTodoItemsAdapter.TABLE_TODO_ITEMS_ID
					+ "= ?";
			String selectConditionsArgs[] = { uri.getLastPathSegment() };
			douiSQLiteOpenHelper.getTableTodoItemsAdapter().update(values,
					selectConditions, selectConditionsArgs);
		}
			break;
		case TODO_ITEM_URI_ID:{
			String selectConditions = TableTodoItemsAdapter.TABLE_TODO_ITEMS_ID	+ "= ?";
			String selectConditionsArgs[] = { uri.getLastPathSegment() };
			result = douiSQLiteOpenHelper.getTableTodoItemsAdapter().update(
					values, selectConditions, selectConditionsArgs);
		}
			break;
		default:
			Log.e(this.getClass().getName(),
					"Unknown URI type passed to query(...): " + uriType);
			throw new IllegalArgumentException(
					"Unknown URI type passed to query(...): " + uriType);
		}
		return result;
	}

}
