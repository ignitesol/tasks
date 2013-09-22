package co.usersource.doui.database.adapters;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Classes that implement this interface provide methods which allow simplify
 * table management routines.
 * 
 * @author rsh
 * 
 */
public interface ITableAdapter {

	/**
	 * Method to create table and insert required data.
	 * 
	 * @param database
	 *            database where table will be created.
	 */
	public abstract void onCreate(SQLiteDatabase database);

	public abstract long insert(ContentValues values);

	public abstract int delete(String arg1, String[] arg2);

	public abstract int update(ContentValues values, String selection,
			String[] selectionArgs);

	public abstract Cursor query(String[] projection, String selection,
			String[] selectionArgs, String sortOrder);

}