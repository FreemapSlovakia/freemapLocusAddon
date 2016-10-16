package sk.freemap.locus.addon.routePlanner.mapDownloader;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, final Intent intent) {
		scheduleUpdateCheck(context);
	}

	public static void scheduleUpdateCheck(final Context context) {
		final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		
		final Intent actionIntent = new Intent(context, UpdateCheckService.class);
		
		if (PendingIntent.getService(context, 0, actionIntent, PendingIntent.FLAG_NO_CREATE) == null) {
			Log.d("sk.freemap.locus.addon.routePlanne", "scheduling update check");
			alarmManager.setInexactRepeating(
					AlarmManager.RTC,
					System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR,
					2L * AlarmManager.INTERVAL_DAY,
			        PendingIntent.getService(context, 0, actionIntent, 0));
		} else {
			Log.d("sk.freemap.locus.addon.routePlanne", "update check already scheduled");
		}
	}

}
