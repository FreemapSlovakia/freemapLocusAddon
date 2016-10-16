package sk.freemap.locus.addon.routePlanner.mapDownloader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.util.Map;

import org.json.JSONException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class UpdateCheckService extends Service {
	
	private static final String TAG = "sk.freemap.locus.addon.routePlanner";
	private static final String LAST_UPDATE_TIME_FILE = "last-update-time-2";

	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		new DownloadAsyncTask("http://proxy.freemap.sk/locus/2/resources", new DownloadResultHandler() {
			@Override
			public void handleResult(final String result) {
				workHard(result);
			}

			@Override
			public void handleException(final IOException e) {
				Log.e(TAG, "Error in communication with server: " + e.getMessage(), e);
			}
		}) {
			@Override
			protected void setupCnnection(final HttpURLConnection con) {
				super.setupCnnection(con);
				con.setRequestProperty("Accept", "application/json");
			}
		}.execute();

		return START_STICKY;
	}

	private void notifyUpdateFound() {
		final Context context = getApplicationContext();
		final PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, DownloaderActivity.class), 0);
		
		final String updateAvailableDetail = context.getString(sk.freemap.locus.addon.routePlanner.R.string.update_available_text);
		
		final String updateAvailable = context.getString(sk.freemap.locus.addon.routePlanner.R.string.update_available);
		final Notification notification = new Notification(sk.freemap.locus.addon.routePlanner.R.drawable.ic_launcher, updateAvailable, System.currentTimeMillis());
		notification.flags =  Notification.FLAG_AUTO_CANCEL;
		// notification.defaults |= Notification.DEFAULT_SOUND;
		notification.setLatestEventInfo(context, updateAvailable, updateAvailableDetail, contentIntent);
		final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(533, notification);
	}
	
	@Override
	public IBinder onBind(final Intent intent) {
		return null;
	}
	
	private void workHard(final String result) {
		try {
			final Map<String, FileInfo> serverFileInfoMap = LocalDatabase.createFileInfoMap(result);
			
			final LocalDatabase localDatabase = new LocalDatabase(UpdateCheckService.this);
			
			long lastUpdateFoundTime;
			
			try {
				final Reader reader = new InputStreamReader(getApplicationContext().openFileInput(LAST_UPDATE_TIME_FILE));
				final char[] buf = new char[20];
				final int n = reader.read(buf);
				lastUpdateFoundTime = n == 20 ? 0 : Long.parseLong(new String(buf, 0, n));
				reader.close();
			} catch (final FileNotFoundException e) {
				lastUpdateFoundTime = 0; // Long.MAX_VALUE; // for the first time play as if update has been checked already
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}

			boolean updateFound = false;
			
			try {
				for (final FileInfo fileInfo : serverFileInfoMap.values()) {
					final FileInfo localFileInfo = localDatabase.getFileInfo(fileInfo.getId());
					
					if (localFileInfo != null) {
						if (localFileInfo.getDate() < fileInfo.getDate() && fileInfo.getDate() > lastUpdateFoundTime / 1000L) {
							updateFound = true;
							notifyUpdateFound();
							break;
						}
					}
				}
			} catch (final IOException e) {
				Log.e(TAG, "IOException: " + e.getMessage(), e);
			}
			
			if (updateFound) {
				try {
					final Writer w = new OutputStreamWriter(getApplicationContext().openFileOutput(LAST_UPDATE_TIME_FILE, MODE_PRIVATE));
					w.write(Long.toString(System.currentTimeMillis()));
					w.close();
				} catch (final IOException e) {
					throw new RuntimeException();
				}
			}
		} catch (final JSONException e) {
			Log.e(TAG, "Error parsing response from server.", e);
		}
	}

}
