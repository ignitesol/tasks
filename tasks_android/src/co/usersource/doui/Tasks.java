/**
 * 
 */
package co.usersource.doui;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import co.usersource.doui.gui.DouiMainActivity;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

/**
 * @author rsh
 *
 */
@ReportsCrashes(formKey = "", // will not be used
mailTo = "taranov.pavel@gmail.com")
public class Tasks extends Application {
	
	/**
	 * Constant for notification area that identifies notifications of the sync
	 * adapter.
	 */
	public static final int SYNC_NOTIFICATION_ID = 0;
	
	@Override
	  public void onCreate() {
	      super.onCreate();

	      // The following line triggers the initialization of ACRA
	      ACRA.init(this);
	  }
	
	/**
	 * Method to place notifications to the Android notification bar.
	 * @param context context should be used to place this notification.
	 * @param id identifier for new notification. Must be unique for different notifications inside application.
	 * @param message message to be placed to the notification area.
	 * */
	public static void placeNotification(Context context, int id, String message) {
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				context).setSmallIcon(R.drawable.app_icon_48)
				.setContentTitle("Tasks notification")
				.setContentText(message);
		Intent resultIntent = new Intent(context, DouiMainActivity.class);
		TaskStackBuilder stackBuilder = TaskStackBuilder.from(context);
		stackBuilder.addParentStack(DouiMainActivity.class);
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(id,
				mBuilder.getNotification());
	}

}
