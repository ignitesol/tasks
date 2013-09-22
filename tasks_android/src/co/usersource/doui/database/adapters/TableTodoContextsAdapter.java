/**
 * 
 */
package co.usersource.doui.database.adapters;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @author rsh
 * 
 */
public class TableTodoContextsAdapter implements ITableAdapter {

	/** Table with contexts (@NAME items). */
	public static final String TABLE_TODO_CONTEXTS = "todo_contexts";
	/** Table with contexts (@NAME items). Primary key. */
	public static final String TABLE_TODO_CONTEXTS_ID = "_id";
	/** Table with contexts (@NAME items). Name. */
	public static final String TABLE_TODO_CONTEXTS_NAME = "name";
	/** Table with contexts (@LAST_UPDATE items). Last update. */
	public static final String TABLE_TODO_CONTEXTS_LAST_UPDTAE = "last_update";
	/** Table with contexts (@LAST_UPDATE items). Unique object key from server. */
	public static final String TABLE_TODO_CONTEXTS_OBJECT_KEY = "object_key";
	/** Table with contexts (@NAME items). Create statement. */
	public static final String STR_CREATE_TABLE_TODO_CONTEXTS = "create table "
			+ TABLE_TODO_CONTEXTS + "(" + TABLE_TODO_CONTEXTS_ID
			+ " integer primary key autoincrement, " + TABLE_TODO_CONTEXTS_NAME
			+ " TEXT,"
			+ TABLE_TODO_CONTEXTS_LAST_UPDTAE + " timestamp not null default current_timestamp, "
			+ TABLE_TODO_CONTEXTS_OBJECT_KEY + " TEXT "
			+ ");";

	/** Trigger for table with contexts to update timestamp (@NAME items). Create statement. */
	public static final String STR_CREATE_TRIGGER_TODO_CONTEXTS = "CREATE TRIGGER UPDATE_" + TABLE_TODO_CONTEXTS + " BEFORE UPDATE ON " + TABLE_TODO_CONTEXTS + 
			  " BEGIN UPDATE " + TABLE_TODO_CONTEXTS + " SET " + TABLE_TODO_CONTEXTS_LAST_UPDTAE + " = current_timestamp " + 
		      " WHERE rowid = new.rowid;  END";

	private SQLiteOpenHelper sqliteOpenHelper;

	public TableTodoContextsAdapter(SQLiteOpenHelper sqliteOpenHelper) {
		this.sqliteOpenHelper = sqliteOpenHelper;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * co.usersource.doui.database.adapters.ITableAdapter#onCreate(
	 * android.database.sqlite.SQLiteDatabase)
	 */
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(STR_CREATE_TABLE_TODO_CONTEXTS);
		database.execSQL(STR_CREATE_TRIGGER_TODO_CONTEXTS);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * co.usersource.doui.database.adapters.ITableAdapter#insert(android
	 * .content.ContentValues)
	 */
	public long insert(ContentValues values) {
		return this.sqliteOpenHelper.getWritableDatabase().insert(
				TABLE_TODO_CONTEXTS, null, values);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * co.usersource.doui.database.adapters.ITableAdapter#delete(java
	 * .lang.String, java.lang.String[])
	 */
	public int delete(String arg1, String[] arg2) {
		SQLiteDatabase database = this.sqliteOpenHelper.getWritableDatabase();
		int result = database.delete(TABLE_TODO_CONTEXTS, arg1, arg2);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * co.usersource.doui.database.adapters.ITableAdapter#update(android
	 * .content.ContentValues, java.lang.String, java.lang.String[])
	 */
	public int update(ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase database = this.sqliteOpenHelper.getWritableDatabase();
		int result = database.update(TABLE_TODO_CONTEXTS, values, selection,
				selectionArgs);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * co.usersource.doui.database.adapters.ITableAdapter#query(java
	 * .lang.String[], java.lang.String, java.lang.String[], java.lang.String)
	 */
	public Cursor query(String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Cursor result = null;
		SQLiteDatabase database = this.sqliteOpenHelper.getReadableDatabase();
		result = database.query(TABLE_TODO_CONTEXTS, projection, selection,
				selectionArgs, null, null, sortOrder);
		return result;
	}

	public ContentValues getContextByName(String contextName) {
		ContentValues result = null;
		String columns[] = { TABLE_TODO_CONTEXTS_ID, TABLE_TODO_CONTEXTS_NAME };
		String selection = TABLE_TODO_CONTEXTS_NAME + " = ?";
		String selectionArgs[] = { contextName };
		Cursor cursor = this.sqliteOpenHelper.getWritableDatabase().query(
				TABLE_TODO_CONTEXTS, columns, selection, selectionArgs, null,
				null, null);
		if (cursor.getCount() > 0) {
			result = new ContentValues();
			cursor.moveToFirst();
			for (String columnName : columns) {
				result.put(columnName,
						cursor.getString(cursor.getColumnIndex(columnName)));
			}
		}
		cursor.close();
		return result;
	}

	/** Query which remove contexts without linked items */
	public void removeEmptyContexts() {
		final String delete_query = "delete from "
				+ TABLE_TODO_CONTEXTS
				+ " where not exists "
				+ "(select 1 from "
				+ TableTodoItemsContextsAdapter.TABLE_TODO_ITEMS_CONTEXTS
				+ " where "
				+ TableTodoItemsContextsAdapter.TABLE_TODO_ITEMS_CONTEXTS
				+ "."
				+ TableTodoItemsContextsAdapter.TABLE_TODO_ITEMS_CONTEXTS_FK_TODO_CONTEXTS
				+ "=" + TABLE_TODO_CONTEXTS + "." + TABLE_TODO_CONTEXTS_ID+")";
		SQLiteDatabase database = this.sqliteOpenHelper.getWritableDatabase();
		database.execSQL(delete_query);
	}

	public Cursor queryContextItems(String contextName) {
		SQLiteDatabase database = this.sqliteOpenHelper.getWritableDatabase();
		String sql = "select "
				+ TableTodoItemsAdapter.TABLE_TODO_ITEMS
				+ ".* from "
				+ TABLE_TODO_CONTEXTS
				+ " join "
				+ TableTodoItemsContextsAdapter.TABLE_TODO_ITEMS_CONTEXTS
				+ " on "
				+ TableTodoItemsContextsAdapter.TABLE_TODO_ITEMS_CONTEXTS_FK_TODO_CONTEXTS
				+ "="
				+ TABLE_TODO_CONTEXTS+"."+TABLE_TODO_CONTEXTS_ID
				+ " join "
				+ TableTodoItemsAdapter.TABLE_TODO_ITEMS
				+ " on "
				+ TableTodoItemsContextsAdapter.TABLE_TODO_ITEMS_CONTEXTS_FK_TODO_ITEMS
				+ "=" + TableTodoItemsAdapter.TABLE_TODO_ITEMS + "."
				+ TableTodoItemsAdapter.TABLE_TODO_ITEMS_ID + " where "+TABLE_TODO_CONTEXTS_NAME+"=?";
		String selectionArgs[] = {contextName};
		return database.rawQuery(sql, selectionArgs);
	}
}
