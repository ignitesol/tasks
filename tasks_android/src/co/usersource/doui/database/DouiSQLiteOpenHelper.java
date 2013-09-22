/**
 * 
 */
package co.usersource.doui.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import co.usersource.doui.database.adapters.TableTodoCategoriesAdapter;
import co.usersource.doui.database.adapters.TableTodoContextsAdapter;
import co.usersource.doui.database.adapters.TableTodoItemsAdapter;
import co.usersource.doui.database.adapters.TableTodoItemsContextsAdapter;
import co.usersource.doui.database.adapters.TableTodoStatusAdapter;
import co.usersource.doui.database.upgradeHelper.DouiDatabaseUpgradeHelper_1_2;

/**
 * @author rsh
 * 
 *         This class intended to provide access and creation routines for Doui
 *         program database.
 */
public class DouiSQLiteOpenHelper extends SQLiteOpenHelper {

	/** Database name. */
	public static final String DATABASE_NAME = "todo.db";
	/** Version for upgrade routines. */
	public static final int DATABASE_VERSION = 2;


	/** Helper member to access Todo categories table. */
	private TableTodoCategoriesAdapter tableTodoCategoriesAdapter;
	/** Helper member to access TodoItems table. */
	private TableTodoItemsAdapter tableTodoItemsAdapter;
	/** Helper member to access TodoContexts table. */
	private TableTodoContextsAdapter tableTodoContextsAdapter;
	/** Helper member to access TodoStatuses table. */
	private TableTodoStatusAdapter tableTodoStatusAdapter;
	
	/**
	 * @return the tableTodoStatusAdapter
	 */
	public TableTodoStatusAdapter getTableTodoStatusAdapter() {
		return tableTodoStatusAdapter;
	}

	/** Helper member to access Items to Contexts linking table. */
	private TableTodoItemsContextsAdapter tableTodoItemsContextsAdapter;

	/**
	 * @return the tableTodoContextsAdapter
	 */
	public TableTodoContextsAdapter getTableTodoContextsAdapter() {
		return tableTodoContextsAdapter;
	}

	/**
	 * @return the tableTodoItemsContextsAdapter
	 */
	public TableTodoItemsContextsAdapter getTableTodoItemsContextsAdapter() {
		return tableTodoItemsContextsAdapter;
	}

	/**
	 * @return the tableTodoItemsAdapter
	 */
	public TableTodoItemsAdapter getTableTodoItemsAdapter() {
		return tableTodoItemsAdapter;
	}

	/**
	 * @return the tableTodoCategoriesAdapter
	 */
	public TableTodoCategoriesAdapter getTableTodoCategoriesAdapter() {
		return tableTodoCategoriesAdapter;
	}

	/**
	 * Constructor.
	 * */
	public DouiSQLiteOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		tableTodoCategoriesAdapter = new TableTodoCategoriesAdapter(this);  
		tableTodoItemsAdapter = new TableTodoItemsAdapter(this);
		tableTodoContextsAdapter = new TableTodoContextsAdapter(this);
		tableTodoItemsContextsAdapter = new TableTodoItemsContextsAdapter(this);
		tableTodoStatusAdapter = new TableTodoStatusAdapter(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite
	 * .SQLiteDatabase)
	 */
	@Override
	public void onCreate(SQLiteDatabase database) {
		tableTodoCategoriesAdapter.onCreate(database);
		tableTodoContextsAdapter.onCreate(database);
		tableTodoItemsAdapter.onCreate(database);
		tableTodoItemsContextsAdapter.onCreate(database);
		tableTodoStatusAdapter.onCreate(database);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite
	 * .SQLiteDatabase, int, int)
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if(oldVersion == 0 && (newVersion == 1 || newVersion == 2))
		{
			onCreate(db);
		}
		if(oldVersion == 1 && newVersion == 2)
		{
			DouiDatabaseUpgradeHelper_1_2 upgradeHelper = new DouiDatabaseUpgradeHelper_1_2();
			try{
				upgradeHelper.onUpgrade(this, db, oldVersion, newVersion);
			}catch(Exception e)
			{
				Log.e(this.getClass().getName(), "OnUpgrade exception: "+ e.getMessage());
			}
		}
		
	}

}
