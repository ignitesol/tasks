package co.usersource.doui.gui;

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import co.usersource.doui.DouiContentProvider;
import co.usersource.doui.R;
import co.usersource.doui.database.adapters.TableTodoCategoriesAdapter;
import co.usersource.doui.database.adapters.TableTodoContextsAdapter;
import co.usersource.doui.database.adapters.TableTodoStatusAdapter;
import co.usersource.doui.sync.SyncAdapter;
import co.usersource.doui.sync.SyncFinishHandler;

public class DouiMainActivity extends ListActivity  
	implements AccountManagerCallback<Bundle>{
	private SimpleCursorAdapter adapter;
	private Cursor cursorToDoCategories;
	private Cursor cursorStatuses;
	private Cursor cursorContexts;
	private ImageButton imbtCategories;
	private MergeCursor mergeCursor;
	private ImageButton imbtHelp;
	

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		fillList();
		imbtCategories = (ImageButton) findViewById(R.id.imbtCategories);
		imbtCategories.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				Intent i = new Intent(DouiMainActivity.this,
						DouiTodoCategoriesManagerActivity.class);
				startActivity(i);
			}
		});
		
		imbtHelp = (ImageButton) findViewById(R.id.imbtHelp);
		imbtHelp.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				Intent helpIntent = new Intent(DouiMainActivity.this,
						DouiHelpActivity.class);
				startActivity(helpIntent);
			}
		});
		loadPrefences();
	}
	
	private void loadPrefences()
	{
		PreferenceManager.setDefaultValues(this, R.xml.todo_preferences, false);
		Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
		AccountManager accountManager = AccountManager.get(getApplicationContext());

		Account[] accounts = accountManager.getAccountsByType("com.google");
		if(accounts.length > 0)
		{
			prefEditor.putString(getApplicationContext().getString(R.string.prefSyncAccount_Key), accounts[0].name);
			accountManager.getAuthToken(accounts[0], "ah", null, this, this, null);
		}
		else
		{
			prefEditor.putBoolean(getApplicationContext().getString(R.string.prefIsSyncable_Key), false);
		}
		prefEditor.apply();
	}
	
	public void run(AccountManagerFuture<Bundle> result) {
		Bundle bundle;
		try {
			bundle = result.getResult();
			Intent intent = (Intent) bundle.get(AccountManager.KEY_INTENT);
			if (null != intent) {
				startActivity(intent);
			} else {
				ContentResolver.setIsSyncable(new Account(bundle.getString("authAccount"), "com.google"), DouiContentProvider.AUTHORITY, 1);
				Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
				prefEditor.putString(getApplicationContext().getString(R.string.prefSyncAccount_Key), bundle.getString("authAccount"));
				if(ContentResolver.getSyncAutomatically(new Account(bundle.getString("authAccount"), "com.google"), DouiContentProvider.AUTHORITY))
				{
					prefEditor.putBoolean(getApplicationContext().getString(R.string.prefIsSyncable_Key), true);
				}
				else{
					prefEditor.putBoolean(getApplicationContext().getString(R.string.prefIsSyncable_Key), false);
				}
				prefEditor.apply();
			}
		} catch (OperationCanceledException e) {
			Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
			prefEditor.putBoolean(getApplicationContext().getString(R.string.prefIsSyncable_Key), false);
			prefEditor.apply();
			e.printStackTrace();
			
		} catch (AuthenticatorException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onRestart() {
		fillList();
		super.onRestart();
	}

	private void fillList() {
		String[] from = new String[] { "img_id",
				TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_NAME,
				"TABLE_NAME",
				TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_ID };
		int[] to = new int[] { R.id.icon, R.id.label };

		ContentResolver cr = getContentResolver();
		String categoryProjection[] = {
				R.drawable.ic_category + " as img_id",
				TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_NAME,
				"'" + TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES
						+ "' as TABLE_NAME",
				TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_ID };
		String selectCondition = TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_NAME
				+ "<>? and " + TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_IS_DELETED + " = 0 ";
		String[] selectConditionArgs = { TableTodoCategoriesAdapter.STR_NONE_CATEGORY_NAME };

		cursorToDoCategories = cr.query(
				DouiContentProvider.TODO_CATEGORIES_URI, categoryProjection,
				selectCondition, selectConditionArgs,
				TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_NAME);

		String statusProjection[] = {
				R.drawable.ic_status + " as img_id",
				TableTodoStatusAdapter.TABLE_TODO_STATUSES_NAME,
				"'" + TableTodoStatusAdapter.TABLE_TODO_STATUSES
						+ "' as TABLE_NAME",
				TableTodoStatusAdapter.TABLE_TODO_STATUSES_ID };
		cursorStatuses = cr.query(DouiContentProvider.TODO_STATUSES_URI,
				statusProjection, null, null,
				TableTodoStatusAdapter.TABLE_TODO_STATUSES_ID);

		String contextProjection[] = {
				R.drawable.ic_context + " as img_id",
				TableTodoContextsAdapter.TABLE_TODO_CONTEXTS_NAME,
				"'" + TableTodoContextsAdapter.TABLE_TODO_CONTEXTS
						+ "' as TABLE_NAME",
				TableTodoContextsAdapter.TABLE_TODO_CONTEXTS_ID };
		cursorContexts = cr.query(DouiContentProvider.TODO_CONTEXTS_URI,
				contextProjection, null, null,
				TableTodoContextsAdapter.TABLE_TODO_CONTEXTS_NAME);

		Cursor cursors[] = { cursorStatuses, cursorToDoCategories,
				cursorContexts };
		mergeCursor = new MergeCursor(cursors);

		adapter = new SimpleCursorAdapter(getApplicationContext(),
				R.layout.todo_list_row, mergeCursor, from, to, 0);

		setListAdapter(adapter);
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		mergeCursor.moveToPosition(position);
		String tableName = mergeCursor.getString(mergeCursor
				.getColumnIndex("TABLE_NAME"));
		Intent i = new Intent(this, DouiTodoListActivity.class);
		Uri todoUri = null;
		if (tableName.equals(TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES)) {
			todoUri = Uri.parse(DouiContentProvider.TODO_CATEGORIES_URI + "/"
					+ id + "/" + DouiContentProvider.TODO_PATH);

		} else if (tableName.equals(TableTodoStatusAdapter.TABLE_TODO_STATUSES)) {
			todoUri = Uri.parse(DouiContentProvider.TODO_STATUSES_URI + "/"
					+ id + "/" + DouiContentProvider.TODO_PATH);

		} else if (tableName
				.equals(TableTodoContextsAdapter.TABLE_TODO_CONTEXTS)) {
			Cursor mainCursor = (Cursor) l.getItemAtPosition(position);
			mainCursor.moveToPosition(position);
			String contextName = mainCursor.getString(1);
			todoUri = Uri.parse(DouiContentProvider.TODO_CONTEXTS_URI
					.toString()
					+ "/"
					+ contextName
					+ "/"
					+ DouiContentProvider.TODO_PATH);

		}
		i.putExtra(DouiTodoListActivity.STR_TODO_LIST_URI_EXT, todoUri);
		SyncFinishHandler syncHandler = new SyncFinishHandler(this);
		syncHandler.setFinishActivity(i);
		SyncAdapter.requestSync(getApplicationContext());
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.ListActivity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		if (!adapter.getCursor().isClosed()) {
			adapter.getCursor().close();
		}
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.todo_main_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.mi_settings:
	        	Intent i = new Intent(this, DouiSettingsActivity.class);
	        	startActivity(i);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	
}