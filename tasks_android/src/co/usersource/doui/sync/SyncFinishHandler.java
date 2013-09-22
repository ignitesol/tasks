package co.usersource.doui.sync;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;


public class SyncFinishHandler extends BroadcastReceiver {
	
	private Intent finishActivity;
	private Activity parentActivity;
	private ProgressDialog progressDlg;
	
	public SyncFinishHandler(Activity parent) {
		IntentFilter syncIntentFilter = new IntentFilter(SyncAdapter.ACTION_SYNC_FINISHED);
		parentActivity = parent;
		parentActivity.registerReceiver(this, syncIntentFilter);
		this.ShowProgressBar(true);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		parentActivity.startActivity(finishActivity);
		parentActivity.unregisterReceiver(this);
		this.ShowProgressBar(false);
	}
	
	public void  setFinishActivity(Intent activity) {
		finishActivity = activity;
	}
	
	
	public void ShowProgressBar(Boolean isShow)
	{
		if(isShow)
		{
			progressDlg = ProgressDialog.show(parentActivity, "Please wait", "Loading...");
			progressDlg.setCancelable(true);
		}
		else{
			progressDlg.dismiss();
		}
	}

}
