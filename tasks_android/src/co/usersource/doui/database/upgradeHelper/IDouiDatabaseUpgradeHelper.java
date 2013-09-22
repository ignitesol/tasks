/**
 * 
 */
package co.usersource.doui.database.upgradeHelper;

import co.usersource.doui.database.DouiSQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;

/**
 * @author rsh Objects which implement this interface contain routines, required
 *         for some database upgrade.
 */
public interface IDouiDatabaseUpgradeHelper {
	/**
	 * Routine to be called to perform upgrade.
	 * 
	 * @param dbOpenHelper
	 *            helper which is used to access to database.
	 * @param db
	 *            database to be upgraded.
	 * @param oldVersion
	 *            previous db version.
	 * @param newVersion
	 *            next database version.
	 * */
	public void onUpgrade(DouiSQLiteOpenHelper dbOpenHelper, SQLiteDatabase db,
			int oldVersion, int newVersion) throws Exception;
}
