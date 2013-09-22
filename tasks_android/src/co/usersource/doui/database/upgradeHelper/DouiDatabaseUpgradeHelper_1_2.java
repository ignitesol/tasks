/**
 * 
 */
package co.usersource.doui.database.upgradeHelper;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import co.usersource.doui.database.DouiSQLiteOpenHelper;
import co.usersource.doui.database.adapters.TableTodoCategoriesAdapter;
import co.usersource.doui.database.adapters.TableTodoStatusAdapter;

/**
 * @author rsh
 * 
 */
public class DouiDatabaseUpgradeHelper_1_2 implements
		IDouiDatabaseUpgradeHelper {

	/*-----------------------------------------------------------*/
	/* Statuses */
	/** Table with status items. */
	public static final String TABLE_TODO_STATUSES = "todo_statuses";
	/** Table with status items. Primary key. */
	public static final String TABLE_TODO_STATUSES_ID = "_id";
	/** Table with status items. Name. */
	public static final String TABLE_TODO_STATUSES_NAME = "name";
	/** Table with status items. LastUpdate. */
	public static final String TABLE_TODO_STATUSES_LAST_UPDATE = "last_update";
	/** Table with status items. Create statement. */
	public static final String STR_CREATE_TABLE_TODO_STATUSES = "create table "
			+ TABLE_TODO_STATUSES+ "(" + TABLE_TODO_STATUSES_ID
			+ " integer primary key autoincrement, " + TABLE_TODO_STATUSES_NAME
			+ " TEXT, " 
			+  TABLE_TODO_STATUSES_LAST_UPDATE + "timestamp not null default current_timestamp " 
			+ ");";

	/** Pre-defined array of Statuses. */
	public static final String STR_ARRAY_STATUSES[] = { "Next",
		"Calendar", "Waiting", "Done", "Someday"};
	
	/*-----------------------------------------------------------*/
	/* Categories */

	/** Table with lists of the todo. */
	private static final String TABLE_TODO_CATEGORIES = "todo_categories";
	/** Table with lists of the todo. Primary key. */
	private static final String TABLE_TODO_CATEGORIES_ID = "_id";
	/** Table with lists of the todo. Name of the list. */
	private static final String TABLE_TODO_CATEGORIES_NAME = "name";
	/** Table with lists of the todo. Last update of the list. */
	private static final String TABLE_TODO_CATEGORIES_LAST_UPDATE = "last_update";
	
	/** Table with lists of the todo. Create statement. */
	private static final String STR_CREATE_TABLE_TODO_CATEGORIES = "create table "
			+ TABLE_TODO_CATEGORIES + "(" + TABLE_TODO_CATEGORIES_ID
			+ " integer primary key autoincrement, " + TABLE_TODO_CATEGORIES_NAME
			+ " TEXT," 
			+ TABLE_TODO_CATEGORIES_LAST_UPDATE + "timestamp not null default current_timestamp "
			+ ");";
	private static final String STR_ARRAY_CATEGORIES[] = { "-None-", "Finance and admin", "Health"};
	
	/*-----------------------------------------------------------*/
	/* ToDo Items */
	private static final String STR_TABLE_TODO_ITEMS_TMP = "todo_items_tmp";
	private static final String STR_TABLE_TODO_ITEMS = "todo_items";
	
	/** Table where todo items stored. Primary key. */
	private static final String TABLE_TODO_ITEMS_ID = "_id";
	/** Table where todo items stored. Title. */
	private static final String TABLE_TODO_ITEMS_TITLE = "title";
	/** Table where todo items stored. Text of the todo. */
	private static final String TABLE_TODO_ITEMS_BODY = "body";
	/** Table where todo items stored. Last update of the todo. */
	private static final String TABLE_TODO_ITEMS_LAST_UPDATE = "last_update";
	/** Table where todo items stored. Foreign key to current item status. */
	private static final String TABLE_TODO_ITEMS_FK_STATUS = "fk_status";
	/** Table where todo items stored. Reference to the primary list item. */
	private static final String TABLE_TODO_ITEMS_FK_LIST = "fk_list";
	/** Reference to categories.*/
	private static final String TABLE_TODO_ITEMS_FK_CATEGORY = "fk_category";
	/** Table where todo items stored. Create statement. */
	private static final String STR_CREATE_TABLE_TODO_ITEMS_TMP = "create table "
			+ STR_TABLE_TODO_ITEMS_TMP + "(" + TABLE_TODO_ITEMS_ID
			+ " integer primary key autoincrement, " + TABLE_TODO_ITEMS_TITLE
			+ " TEXT, " + TABLE_TODO_ITEMS_BODY + " TEXT, "
			+ TABLE_TODO_ITEMS_LAST_UPDATE + "timestamp not null default current_timestamp, " 
			+ TABLE_TODO_ITEMS_FK_STATUS + " INTEGER DEFAULT NULL, "
			+ TABLE_TODO_ITEMS_FK_LIST + " INTEGER, " + "FOREIGN KEY("
			+ TABLE_TODO_ITEMS_FK_STATUS + ") REFERENCES "
			+ TableTodoStatusAdapter.TABLE_TODO_STATUSES + "("
			+ TableTodoStatusAdapter.TABLE_TODO_STATUSES_ID + "),"
			+ "FOREIGN KEY(" + TABLE_TODO_ITEMS_FK_LIST + ") REFERENCES "
			+ TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES + "("
			+ TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_ID + ")" + ");";

	/** Query to drop old table with todo items*/
	private static final String STR_DROP_TABLE_TODO_ITEMS = "drop table "+STR_TABLE_TODO_ITEMS;
	
	/** Query to restore todo items table from temporary */
	private static final String STR_RENAME_TABLE_TODO_ITEMS_TMP = "alter table " + STR_TABLE_TODO_ITEMS_TMP + " rename to " + STR_TABLE_TODO_ITEMS;
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * co.usersource.doui.database.upgradeHelper.IDouiDatabaseUpgradeHelper#
	 * onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
	 */
	public void onUpgrade(DouiSQLiteOpenHelper dbOpenHelper, SQLiteDatabase db,
			int oldVersion, int newVersion) throws Exception {
		if (oldVersion != 1 || newVersion != 2) {
			throw new Exception("Wrong database versions: oldVersion="
					+ oldVersion + ", newVersion=" + newVersion);
		}
		// Create statuses
		db.execSQL(STR_CREATE_TABLE_TODO_STATUSES);
		for (int i = 0; i < STR_ARRAY_STATUSES.length; i++) {
			db.execSQL("insert or replace into " + TABLE_TODO_STATUSES + "("
					+ TABLE_TODO_STATUSES_ID + "," + TABLE_TODO_STATUSES_NAME
					+ ") values (" + i  + ",'" + STR_ARRAY_STATUSES[i] + "');");
		}

		// Create new categories table
		db.execSQL(STR_CREATE_TABLE_TODO_CATEGORIES);
		for (int i = 0; i < STR_ARRAY_CATEGORIES.length; i++) {
			db.execSQL("insert or replace into " + TABLE_TODO_CATEGORIES + "("
					+ TABLE_TODO_CATEGORIES_ID + "," + TABLE_TODO_CATEGORIES_NAME
					+ ") values (" + i  + ",'" + STR_ARRAY_CATEGORIES[i] + "');");
		}
		upgradeToDoItems(db);
	}

	private void upgradeToDoItems(SQLiteDatabase db)
	{
		db.execSQL(STR_CREATE_TABLE_TODO_ITEMS_TMP);
		String[] oldToDoPojection = {TABLE_TODO_ITEMS_TITLE, TABLE_TODO_ITEMS_BODY,"isDone", TABLE_TODO_ITEMS_FK_LIST};
		Cursor cursor = db.query(STR_TABLE_TODO_ITEMS, oldToDoPojection, null, null, null, null, null);
		while(!cursor.isLast())
		{
			cursor.moveToNext();
			ContentValues values = new ContentValues();
			values.put(TABLE_TODO_ITEMS_TITLE, cursor.getString(0));
			values.put(TABLE_TODO_ITEMS_BODY, cursor.getString(1));
			int isDone = cursor.getInt(2);
			if(isDone == 1)
			{
				values.put(TABLE_TODO_ITEMS_FK_STATUS, 3); // Make sure that this id is right.
			}
			values.put(TABLE_TODO_ITEMS_FK_CATEGORY, 0); // All new items placed to "-None-" category.
		}
		db.execSQL(STR_DROP_TABLE_TODO_ITEMS);
		db.execSQL(STR_RENAME_TABLE_TODO_ITEMS_TMP);
	}
}
