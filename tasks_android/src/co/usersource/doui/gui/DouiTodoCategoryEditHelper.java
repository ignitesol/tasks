/**
 * 
 */
package co.usersource.doui.gui;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import co.usersource.doui.R;

/**
 * @author rsh
 * 
 */
public class DouiTodoCategoryEditHelper {

	private EditText etItemName;

	/**
	 * @return the etItemName
	 */
	public EditText getEtItemName() {
		return etItemName;
	}

	private TextView tvItemName;
	private ImageButton imbtSave;

	private OnClickListener imbtSaveOnClickListener;
	private OnClickListener imbtDeleteOnClickListener;
	private ImageButton imbtDelete;
	private Activity parent;

	public DouiTodoCategoryEditHelper(Activity parent, View editableRow) {
		tvItemName = (TextView) editableRow.findViewById(R.id.tvItemName);
		etItemName = (EditText) editableRow.findViewById(R.id.etItemName);
		imbtSave = (ImageButton) editableRow.findViewById(R.id.imbtSave);
		imbtDelete = (ImageButton) editableRow.findViewById(R.id.imbtDelete);
		this.parent = parent;
	}

	public void switchEditableRowToEdit() {
		tvItemName.setVisibility(View.GONE);
		etItemName.setVisibility(View.VISIBLE);
		imbtSave.setVisibility(View.VISIBLE);
		imbtDelete.setVisibility(View.VISIBLE);

		etItemName.setText(tvItemName.getText());
		etItemName.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					etItemName.post(new Runnable() {
						public void run() {
							InputMethodManager imm = (InputMethodManager) parent
									.getSystemService(Context.INPUT_METHOD_SERVICE);
							imm.showSoftInput(etItemName,
									InputMethodManager.SHOW_IMPLICIT);
						}
					});
				}
			}
		});
		etItemName.requestFocus();
		etItemName.requestFocusFromTouch();

	}

	public void switchEditableRowToInsert() {
		tvItemName.setVisibility(View.GONE);
		etItemName.setVisibility(View.VISIBLE);
		imbtSave.setVisibility(View.VISIBLE);
		imbtDelete.setVisibility(View.GONE);
		etItemName.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					etItemName.post(new Runnable() {
						public void run() {
							InputMethodManager imm = (InputMethodManager) parent
									.getSystemService(Context.INPUT_METHOD_SERVICE);
							imm.showSoftInput(etItemName,
									InputMethodManager.SHOW_IMPLICIT);
						}
					});
				}
			}
		});
		etItemName.requestFocus();
		etItemName.requestFocusFromTouch();
	}

	public void switchEditableRowToView() {
		tvItemName.setVisibility(View.VISIBLE);
		etItemName.setVisibility(View.GONE);
		imbtSave.setVisibility(View.GONE);
		// imbtCancel.setVisibility(View.GONE);
		imbtDelete.setVisibility(View.GONE);
		InputMethodManager imm = (InputMethodManager) parent
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(etItemName.getWindowToken(), 0);

	}

	public OnClickListener getImbtSaveOnClickListener() {
		return imbtSaveOnClickListener;
	}

	public void setImbtSaveOnClickListener(
			OnClickListener imbtSaveOnClickListener) {
		this.imbtSaveOnClickListener = imbtSaveOnClickListener;
		imbtSave.setOnClickListener(imbtSaveOnClickListener);
	}

	public OnClickListener getImbtDeleteOnClickListener() {
		return imbtDeleteOnClickListener;
	}

	public void setImbtDeleteOnClickListener(
			OnClickListener imbtDeleteOnClickListener) {
		this.imbtDeleteOnClickListener = imbtDeleteOnClickListener;
		imbtDelete.setOnClickListener(imbtDeleteOnClickListener);
	}
}
