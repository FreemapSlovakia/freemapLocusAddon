package sk.freemap.locus.addon.routePlanner.c2dm;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import sk.freemap.locus.addon.routePlanner.mapDownloader.DetailActivity;
import sk.freemap.locus.addon.routePlanner.mapDownloader.DownloadAsyncTask;
import sk.freemap.locus.addon.routePlanner.mapDownloader.DownloadResultHandler;
import sk.freemap.locus.addon.routePlanner.mapDownloader.FileInfo;
import sk.freemap.locus.addon.routePlanner.mapDownloader.LocalDatabase;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 *
 *
 * @author <a href="mailto:m.zdila@mwaysolutions.com">Martin Ždila</a>
 */
public class C2DmReceiver extends BroadcastReceiver {

	private final class RegAsyncTask extends AsyncTask<Void, Void, Exception> {
		private final Context context;
		private final String registrationId;
		private final boolean register;

		private RegAsyncTask(final Context context, final String registrationId, final boolean register) {
			this.context = context;
			this.registrationId = registrationId;
			this.register = register;
		}

		@Override
		protected Exception doInBackground(final Void... params) {
			try {
				final HttpURLConnection con = (HttpURLConnection) new URL("http://proxy.freemap.sk/locus/2/registration").openConnection();
				try {
					con.setDoOutput(true);
					con.setDoInput(false);
					con.setRequestMethod("POST");
					con.setRequestProperty("Content-Type", "application/json");
					final JSONObject jo = new JSONObject().put(register ? "register" : "unregister", new JSONObject().put("registrationId", registrationId));
					final byte[] bytes = jo.toString().getBytes();
					con.setRequestProperty("Content-Length", Integer.toString(bytes.length));
					
					final OutputStream os = con.getOutputStream();
					os.write(bytes);
					os.close();
					
					final int responseCode = con.getResponseCode();
					if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
						return new RuntimeException("Server response: " + con.getResponseMessage());
					}
				} catch (final JSONException e) {
					return e;
				} finally {
					con.disconnect();
				}
			} catch (final IOException e) {
				return e;
			}
			
			return null;
		}

		@Override
		protected void onPostExecute(final Exception e) {
			if (e != null) {
				Toast.makeText(context, "Registration Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
			} else if (register) {
				final Editor editor = context.getSharedPreferences("c2dm", Context.MODE_PRIVATE).edit();
				editor.putString(C2DmConstants.REGISTRATION_ID, registrationId);
				editor.commit();
			}
		}
	}

	private static final String TAG = C2DmReceiver.class.getName();


	@Override
	public void onReceive(final Context context, final Intent intent) {
		if (intent.getAction().equals(C2DmConstants.ACTION_REGISTRATION)) {
			handleRegistration(context, intent);
		} else if (intent.getAction().equals(C2DmConstants.ACTION_RECEIVE)) {
			handleMessage(context, intent);
		}
	}

	/**
	 * 
	 * @param context
	 * @param intent
	 */
	private void handleRegistration(final Context context, final Intent intent) {
		final String registrationId = intent.getStringExtra(C2DmConstants.REGISTRATION_ID);
		if (intent.hasExtra(C2DmConstants.EXTRA_ERROR)) {
			// Registration has failed, should try again later.
			final String error = intent.getStringExtra(C2DmConstants.EXTRA_ERROR);
			Log.e(TAG, "C2DM registration failed: " + error);
			Toast.makeText(context, "C2DM registration failed: " + error, Toast.LENGTH_LONG).show();
		} else if (intent.getStringExtra(C2DmConstants.EXTRA_UNREGISTERED) != null) {
			// unregistration done, new messages from the authorized sender will be rejected
			Log.i(TAG, "unregistered");
			
			final Editor editor = context.getSharedPreferences("c2dm", Context.MODE_PRIVATE).edit();
			editor.remove(C2DmConstants.REGISTRATION_ID);
			editor.commit();
			
			new RegAsyncTask(context, registrationId, false).execute();
		} else if (registrationId != null) {
			Log.i(TAG, registrationId);
			new RegAsyncTask(context, registrationId, true).execute();
		}
	}

	private void handleMessage(final Context context, final Intent intent) {
		final Bundle extras = intent.getExtras();
		final String from = extras.getString(C2DmConstants.EXTRA_FROM);
		final String collapseKey = extras.getString(C2DmConstants.EXTRA_COLLAPSE_KEY);

		Log.i(TAG, "C2DM message: from: " + from + "; collapseKey: " + collapseKey + "; extras: " + extras.toString());
		
		final FileInfo fileInfo;
		final String id;
		final String newResource;
		final LocalDatabase localDatabase = new LocalDatabase(context);
		try {
			id = extras.getString("resource.id");
			newResource = extras.getString("resource.new");
			fileInfo = localDatabase.getFileInfo(id);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		
		if ((fileInfo == null || fileInfo.getDate() == 0) && newResource == null) {
			return;
		}
		
		new DownloadAsyncTask("http://proxy.freemap.sk/locus/2/resources/" + id, new DownloadResultHandler() {
			@Override
			public void handleResult(final String result) {
				final String name;
				final long date;
				try {
					final JSONObject jo = new JSONObject(result);
					name = jo.getString("name");
					date = jo.getLong("date");
				} catch (final JSONException e) {
					Toast.makeText(context, "Error parsing server response:" +  e.getMessage(), Toast.LENGTH_LONG).show();
					return;
				}
				
				final String updateAvailableDetail;
				if (newResource != null) {
					updateAvailableDetail = context.getString(sk.freemap.locus.addon.routePlanner.R.string.new_available_detail, name);
				} else if (fileInfo.getDate() < date) {
					updateAvailableDetail = context.getString(sk.freemap.locus.addon.routePlanner.R.string.update_available_detail, name);
				} else {
					return;
				}
				
				final Intent notificationIntent = new Intent(context, DetailActivity.class);
				notificationIntent.putExtra("id", id);
				final PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
				
				final String updateAvailable = context.getString(sk.freemap.locus.addon.routePlanner.R.string.update_available);
				final Notification notification = new Notification(sk.freemap.locus.addon.routePlanner.R.drawable.ic_launcher, updateAvailable, System.currentTimeMillis());
				notification.flags =  Notification.FLAG_AUTO_CANCEL;
				notification.defaults |= Notification.DEFAULT_SOUND;
				notification.setLatestEventInfo(context, updateAvailable, updateAvailableDetail, contentIntent);
				final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
				notificationManager.notify(532, notification);
			}
			
			@Override
			public void handleException(final IOException e) {
				Toast.makeText(context, "Communication error: " + e.getMessage(), Toast.LENGTH_LONG).show();
			}
		}) {
			@Override
			protected void setupCnnection(final HttpURLConnection con) {
				super.setupCnnection(con);
				con.setRequestProperty("Accept", "application/json");
			}
		}.execute();
	}

}
