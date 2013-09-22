/**
 * 
 */
package co.usersource.doui.database.adapters;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @author rsh Adapter class for the Todo Lists table.
 */
public class TableTodoCategoriesAdapter implements ITableAdapter {

	/** Table with lists of the todo. */
	public static final String TABLE_TODO_CATEGORIES = "todo_categories";
	/** Table with lists of the todo. Primary key. */
	public static final String TABLE_TODO_CATEGORIES_ID = "_id";
	/** Table with lists of the todo. Name of the list. */
	public static final String TABLE_TODO_CATEGORIES_NAME = "name";
	/** Table with lists of the todo. Last update of the list. */
	public static final String TABLE_TODO_CATEGORIES_LAST_UPDATE = "last_update";
	/** Table with lists of the todo. Unique object key from server. */
	public static final String TABLE_TODO_CATEGORIES_OBJECT_KEY = "object_key";
	/** Table with lists of the todo. Flag indicates that record was deleted by user.*/
	public static final String TABLE_TODO_CATEGORIES_IS_DELETED = "is_deleted";
	/** Table with lists of the todo. Create statement. */
	public static final String STR_CREATE_TABLE_TODO_CATEGORIES = "create table "
			+ TABLE_TODO_CATEGORIES + "(" + TABLE_TODO_CATEGORIES_ID
			+ " integer primary key autoincrement, " + TABLE_TODO_CATEGORIES_NAME
			+ " TEXT,"
			+ TABLE_TODO_CATEGORIES_LAST_UPDATE + " timestamp not null default current_timestamp, "
			+ TABLE_TODO_CATEGORIES_OBJECT_KEY  + " TEXT, "
			+ TABLE_TODO_CATEGORIES_IS_DELETED + " int not null default 0 "
			+");";

	/** Table with lists of the todo. Create upodate trigger for update timestamp. */
	public static final String STR_CREATE_TRIGGER_TODO_CATEGORIES = "CREATE TRIGGER UPDATE_" + TABLE_TODO_CATEGORIES + " BEFORE UPDATE ON " + TABLE_TODO_CATEGORIES + 
			  " BEGIN UPDATE " + TABLE_TODO_CATEGORIES + " SET " + TABLE_TODO_CATEGORIES_LAST_UPDATE + " = current_timestamp " + 
		      " WHERE rowid = new.rowid;  END";

	/** Special category assigned to item if no category selected*/
	public static final String STR_NONE_CATEGORY_NAME = "uncategorized";
	/** Pre-defined array of Lists. */
	public static final String STR_ARRAY_CATEGORIES[] = { STR_NONE_CATEGORY_NAME, "Finance and admin", "Health"};

	private SQLiteOpenHelper sqliteOpenHelper;

	public TableTodoCategoriesAdapter(SQLiteOpenHelper sqliteOpenHelper) {
		this.sqliteOpenHelper = sqliteOpenHelper;
	}

	/* (non-Javadoc)
	 * @see co.usersource.doui.database.ITableAdapter#onCreate(android.database.sqlite.SQLiteDatabase)
	 */
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(STR_CREATE_TABLE_TODO_CATEGORIES);
		database.execSQL(STR_CREATE_TRIGGER_TODO_CATEGORIES);
		for (int i = 0; i < STR_ARRAY_CATEGORIES.length; i++) {
			database.execSQL("insert or replace into " + TABLE_TODO_CATEGORIES + "("
					+ TABLE_TODO_CATEGORIES_ID + "," + TABLE_TODO_CATEGORIES_NAME
					+ ") values (" + i  + ",'" + STR_ARRAY_CATEGORIES[i] + "');");
		}
	}

	/* (non-Javadoc)
	 * @see co.usersource.doui.database.ITableAdapter#insert(android.content.ContentValues)
	 */
	public long insert(ContentValues values) {
		return this.sqliteOpenHelper.getWritableDatabase().insert(TABLE_TODO_CATEGORIES,
				null, values);
	}

	/* (non-Javadoc)
	 * @see co.usersource.doui.database.ITableAdapter#delete(java.lang.String, java.lang.String[])
	 */
	public int delete(String arg1, String[] arg2) {
		SQLiteDatabase database = this.sqliteOpenHelper.getWritableDatabase();
		int result = database.delete(TABLE_TODO_CATEGORIES, arg1, arg2);
		return result;
	}

	/* (non-Javadoc)
	 * @see co.usersource.doui.database.ITableAdapter#update(android.content.ContentValues, java.lang.String, java.lang.String[])
	 */
	public int update(ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase database = this.sqliteOpenHelper.getWritableDatabase();
		int result = database.update(TABLE_TODO_CATEGORIES, values, selection, selectionArgs);
		return result;
	}

	/* (non-Javadoc)
	 * @see co.usersource.doui.database.ITableAdapter#query(java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
	 */
	public Cursor query(String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Cursor result = null;
		SQLiteDatabase database = this.sqliteOpenHelper.getReadableDatabase();
		result = database.query(TABLE_TODO_CATEGORIES, projection, selection,
				selectionArgs, null, null, sortOrder);
		return result;
	}
}
