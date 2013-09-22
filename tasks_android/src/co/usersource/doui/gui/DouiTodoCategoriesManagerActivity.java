/**
 * 
 */
package co.usersource.doui.gui;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import co.usersource.doui.DouiContentProvider;
import co.usersource.doui.R;
import co.usersource.doui.database.adapters.TableTodoCategoriesAdapter;
import co.usersource.doui.sync.SyncAdapter;

/**
 * @author rsh
 * 
 */
public class DouiTodoCategoriesManagerActivity extends Activity {
	private Cursor cursorToDoCategories;
	private DouiTodoCategoryEditHelper douiTodoCategoryEditHelper;
	private View addCategoriesView;
	private LinearLayout llItems;
	private LayoutInflater inflater;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.todo_categories_manager);
		llItems = (LinearLayout) findViewById(R.id.llItems);
		inflater = (LayoutInflater) getApplicationContext().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		fillList();

	}

	/** Routines required to load list of categories. */
	private void fillList() {
		llItems.removeAllViews();
		String[] from = new String[] {
				TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_NAME,
				TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_ID };

		String selectCondition = TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_NAME
				+ "<>? and "
				+ TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_IS_DELETED
				+ " = 0";
		String[] selectConditionArgs = { TableTodoCategoriesAdapter.STR_NONE_CATEGORY_NAME };
		ContentResolver cr = getContentResolver();
		cursorToDoCategories = cr.query(
				DouiContentProvider.TODO_CATEGORIES_URI, from, selectCondition,
				selectConditionArgs,
				TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_NAME);
		while (!cursorToDoCategories.isLast()) {
			cursorToDoCategories.moveToNext();
			View categoriesItemView = (View) inflater.inflate(
					R.layout.todo_category_editable_row, llItems, false);

			TextView tvItemName = (TextView) categoriesItemView
					.findViewById(R.id.tvItemName);
			tvItemName
					.setText(cursorToDoCategories.getString(cursorToDoCategories
							.getColumnIndex(TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_NAME)));
			final long itemId = cursorToDoCategories
					.getLong(cursorToDoCategories
							.getColumnIndex(TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_ID));
			categoriesItemView.setOnClickListener(new OnClickListener() {

				public void onClick(View v) {
					onListItemClick(v, itemId);
				}

			});
			llItems.addView(categoriesItemView);
		}
		cursorToDoCategories.close();
		createAddCategoryControl();
	}

	@Override
	protected void onDestroy() {
		if (!cursorToDoCategories.isClosed()) {
			cursorToDoCategories.close();
		}
		super.onDestroy();
	}

	protected void onListItemClick(View v, long id) {
		final long itemId = id;
		if (douiTodoCategoryEditHelper != null) {
			douiTodoCategoryEditHelper.switchEditableRowToView();
		}
		douiTodoCategoryEditHelper = new DouiTodoCategoryEditHelper(
				DouiTodoCategoriesManagerActivity.this, v);
		douiTodoCategoryEditHelper.switchEditableRowToEdit();
		douiTodoCategoryEditHelper
				.setImbtSaveOnClickListener(new OnClickListener() {

					public void onClick(View v) {
						String newCategoryName = douiTodoCategoryEditHelper
								.getEtItemName().getText().toString();
						ContentValues values = new ContentValues();
						values.put(
								TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_ID,
								itemId);
						values.put(
								TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_NAME,
								newCategoryName);
						saveItem(values);
					}
				});
		douiTodoCategoryEditHelper
				.setImbtDeleteOnClickListener(new OnClickListener() {

					public void onClick(View v) {
						Uri uriTodoItemsInCategory = Uri
								.parse(DouiContentProvider.TODO_CATEGORIES_URI
										.toString()
										+ "/"
										+ itemId
										+ "/"
										+ DouiContentProvider.TODO_PATH);
						Cursor cursor = getContentResolver().query(
								uriTodoItemsInCategory, null, null, null, null);
						if (cursor.getCount() <= 0) {
							Uri updateUri = Uri
									.parse(DouiContentProvider.TODO_CATEGORIES_URI
											.toString() + "/" + itemId);
							ContentValues valueForUpdate = new ContentValues();
							valueForUpdate
									.put(TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_IS_DELETED,
											1);
							getContentResolver().update(updateUri,
									valueForUpdate, null, null);
							fillList();
							SyncAdapter.requestSync(getApplicationContext());
						} else {
							Toast toast = Toast
									.makeText(
											getApplicationContext(),
											"Unable to delete: Category contain items.",
											Toast.LENGTH_LONG);
							toast.show();
						}
						cursor.close();
					}
				});

	}

	/**
	 * Save category item and switch editors to view mode.
	 * 
	 * @param values
	 *            values to be updated. If values contain
	 *            TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_ID then item
	 *            will be updated, otherwise inserted.
	 *            TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_NAME checked
	 *            to be unique.
	 * */
	private void saveItem(ContentValues values) {
		String itemId = values
				.getAsString(TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_ID);
		String newName = values
				.getAsString(TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_NAME);
		Uri selectUri = DouiContentProvider.TODO_CATEGORIES_URI;
		String selectCondition = TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_NAME
				+ "=?";
		String[] selectConditionArg = { newName };
		Cursor cursor = getContentResolver().query(selectUri, null,
				selectCondition, selectConditionArg, null);
		if (cursor.getCount() <= 0) {
			if (null == itemId || itemId.equals("")) {
				Uri insertUri = DouiContentProvider.TODO_CATEGORIES_URI;
				getContentResolver().insert(insertUri, values);
				douiTodoCategoryEditHelper.switchEditableRowToView();
				fillList();
			} else {
				Uri updateUri = Uri
						.parse(DouiContentProvider.TODO_CATEGORIES_URI
								.toString() + "/" + itemId);
				getContentResolver().update(updateUri, values, null, null);
				douiTodoCategoryEditHelper.switchEditableRowToView();
				fillList();

			}
			SyncAdapter.requestSync(getApplicationContext());
		} else {
			Toast toast = Toast.makeText(getApplicationContext(), "Item "
					+ newName + " already exists.\nEnter another name.",
					Toast.LENGTH_LONG);
			toast.show();
		}
		cursor.close();
	}

	/** Utility function to create most-bottom control called "add category" */
	private void createAddCategoryControl() {
		addCategoriesView = (View) inflater.inflate(
				R.layout.todo_category_editable_row, llItems, false);
		addCategoriesView.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if (douiTodoCategoryEditHelper != null) {
					douiTodoCategoryEditHelper.switchEditableRowToView();
				}
				douiTodoCategoryEditHelper = new DouiTodoCategoryEditHelper(
						DouiTodoCategoriesManagerActivity.this, v);
				douiTodoCategoryEditHelper.switchEditableRowToInsert();
				douiTodoCategoryEditHelper
						.setImbtSaveOnClickListener(new OnClickListener() {

							public void onClick(View v) {
								String newCategoryName = douiTodoCategoryEditHelper
										.getEtItemName().getText().toString();
								ContentValues values = new ContentValues();
								values.put(
										TableTodoCategoriesAdapter.TABLE_TODO_CATEGORIES_NAME,
										newCategoryName);
								saveItem(values);
							}
						});
			}
		});
		TextView tvItemName = (TextView) addCategoriesView
				.findViewById(R.id.tvItemName);
		tvItemName.setTextColor(getResources().getColor(
				android.R.color.darker_gray));

		llItems.addView(addCategoriesView);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, DouiMainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
