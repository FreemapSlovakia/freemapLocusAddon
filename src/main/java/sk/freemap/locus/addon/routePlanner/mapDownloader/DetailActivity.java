package sk.freemap.locus.addon.routePlanner.mapDownloader;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import sk.freemap.locus.addon.routePlanner.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class DetailActivity extends Activity {

	private final static DateFormat DATE_FORMAT = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.SHORT);
	
	private Bundle bundle;
	private TextView nameTextView;
	private TextView dateTextView;
	private TextView versionTextView;
	private TextView descriptionTextView;
	private LinearLayout changesLinearLayout;
	private ProgressBar progressBar;
	private Button actionButton;
	private Button deleteButton;

	private LocalDatabase localDatabase;
	private String result;
	
	private Intent serviceIntent;
	private DownloadController downloadController;

	private String id;
	
	
	private final ServiceConnection conn = new ServiceConnection() {
		@Override
		public void onServiceConnected(final ComponentName name, final IBinder binder) {
			downloadController = (DownloadController) binder;
			downloadController.setProgressNotifier(new ProgressNotifier() {
				@Override
				public void onProgress(final String id, final int pct, final Set<String> pendingIds) {
					if (result != null) {
						final boolean contains = pendingIds.contains(DetailActivity.this.id);
						actionButton.post(new Runnable() {
							@Override
							public void run() {
								actionButton.setEnabled(!contains);
								deleteButton.setEnabled(!contains);
							}
						});
						
						DetailActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								if (pct == Integer.MAX_VALUE) {
									drawGui();
								}
							}
						});
					}
				}
			});
			downloadController.askForCurrentProgressNow();
		}
		
		@Override
		public void onServiceDisconnected(final ComponentName name) {
			if (result != null) {
				DetailActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						drawGui();
					}
				});
			}
			
			downloadController.setProgressNotifier(null);
			downloadController = null;
			bindService(serviceIntent, conn, 0); // rebind, because disconnection unbinds
		}
	};

	
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		serviceIntent = new Intent(this, DownloadService.class);

		result = (String) getLastNonConfigurationInstance();
		
		localDatabase = new LocalDatabase(DetailActivity.this);

		final Intent intent = getIntent();
		id = intent.getStringExtra("id");

		setContentView(R.layout.detail);
		
		nameTextView = (TextView) findViewById(R.id.textView_name);
		dateTextView = (TextView) findViewById(R.id.textView_date);
		versionTextView = (TextView) findViewById(R.id.textView_version);
		descriptionTextView = (TextView) findViewById(R.id.textView_description);
		changesLinearLayout = (LinearLayout) findViewById(R.id.linearLayout_changes);
		progressBar = (ProgressBar) findViewById(R.id.progressBar_loadingDetails);
		actionButton = (Button) findViewById(R.id.button_action);
		deleteButton = (Button) findViewById(R.id.button_delete);
		
		deleteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				new AlertDialog.Builder(DetailActivity.this)
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setTitle(R.string.detail_delete_confirm_title)
						.setMessage(R.string.detail_delete_confirm_message)
						.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog, final int which) {
								final String absoluteLocusPath = Utils.findLocusDir(DetailActivity.this).getAbsolutePath();
								try {
									final String[] files = localDatabase.getFiles(id);
									for (int i = files.length - 1; i >= 0; i--) { // in reverse order to delete deepmost first
										new File(files[i].replace("$LOCUS", absoluteLocusPath)).delete(); // will skip nonempty directories
									}
									localDatabase.deleteFileInfo(id);
								} catch (final IOException e) {
									Toast.makeText(DetailActivity.this, "Error reading file list:" +  e.getMessage(), Toast.LENGTH_LONG).show();
								}
								
								drawGui();
								dialog.dismiss();
							}
						})
						.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog, final int which) {
								dialog.cancel();
							}
						}).show();
			}
		});
		
		actionButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				final Intent dlIntent = new Intent(DetailActivity.this, DownloadService.class);
				dlIntent.putExtras(bundle);
				startService(dlIntent);
			}
		});

		if (result == null) {
			new DownloadAsyncTask("http://proxy.freemap.sk/locus/2/resources/" + id, new DownloadResultHandler() {
				@Override
				public void handleResult(final String result) {
					DetailActivity.this.result = result;
					initGui();
				}
	
				@Override
				public void handleException(final IOException e) {
					Toast.makeText(DetailActivity.this, "Communication error: " + e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}) {
				@Override
				protected void setupCnnection(final HttpURLConnection con) {
					super.setupCnnection(con);
					con.setRequestProperty("Accept", "application/json");
				}
			}.execute();
		} else {
			initGui();
		}
	}
	
	
	private void initGui() {
		drawGui();
		
		if (downloadController != null) {
			downloadController.askForCurrentProgressNow();
		}
	}


	private void drawGui() {
		FileInfo localFileInfo;
		try {
			localFileInfo = localDatabase.getFileInfo(id);
		} catch (final IOException e) {
			localFileInfo = null; // TODO log
			Log.e("DetailActivity", "Error reading local database: " + e.getMessage(), e);
		}
		
		final long localDate = localFileInfo == null ? 0 : localFileInfo.getDate();
		

		try {
			final JSONObject jo = new JSONObject(result);

			final long serverDate = jo.optLong("date", 0);
			if (serverDate == 0) {
				actionButton.setVisibility(Button.GONE);
				deleteButton.setVisibility(Button.VISIBLE);
			} else if (localDate == 0) {
				actionButton.setText(R.string.detail_install);
				actionButton.setVisibility(Button.VISIBLE);
				deleteButton.setVisibility(Button.GONE);
			} else if (localDate < serverDate) {
				actionButton.setText(R.string.detail_update);
				actionButton.setVisibility(Button.VISIBLE);
				deleteButton.setVisibility(Button.VISIBLE);
			} else {
				actionButton.setText(R.string.detail_reinstall);
				actionButton.setVisibility(Button.VISIBLE);
				deleteButton.setVisibility(Button.VISIBLE);
			}
			
			nameTextView.setText(jo.getString("name"));
			versionTextView.setText(jo.getString("version"));
			dateTextView.setText(DATE_FORMAT.format(new Date(jo.getLong("date") * 1000)));
			descriptionTextView.setText(Html.fromHtml(jo.getString("description")));
			
			final FileInfo serverFileInfo = LocalDatabase.createFileInfo(jo);
			
			bundle = new Bundle();
			bundle.putParcelable("fileInfo", serverFileInfo);
			bundle.putString("url", jo.getString("url"));
			bundle.putString("action", jo.getString("action"));
			bundle.putString("destination", jo.getString("destination"));
			
			final JSONArray changes = jo.getJSONArray("changes");
			
			changesLinearLayout.removeAllViews();
			
			for (int i = 0, n = changes.length(); i < n; i++) {
				final JSONObject change = changes.getJSONObject(i);
				
				final TextView changesTextView = new TextView(DetailActivity.this);
				final String version = change.getString("version");
				final String title = "✔ " + version + " • " + DATE_FORMAT.format(new Date(change.getLong("date") * 1000));
				final String description = change.optString("description", null);
				final SpannableString spannableString = new SpannableString(title + (change.has("description") ? (" • " + description) : ""));
				spannableString.setSpan(new ForegroundColorSpan(Color.GREEN), 0, 1, 0);
				spannableString.setSpan(new ForegroundColorSpan(Color.GREEN), version.length() + 3, version.length() + 4, 0);
				
				if (change.has("description")) {
					spannableString.setSpan(new ForegroundColorSpan(Color.GREEN), title.length(), title.length() + 3, 0);
				}
				
				changesTextView.setText(spannableString);
				changesLinearLayout.addView(changesTextView);
			}
			
		} catch (final JSONException e) {
			Toast.makeText(DetailActivity.this, "Error parsing server response:" +  e.getMessage(), Toast.LENGTH_LONG).show();
			finish();
		}
		
		actionButton.setEnabled(true);
		deleteButton.setEnabled(true);
		progressBar.setVisibility(ProgressBar.GONE);
	}
	
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return result;
	}
	
	
	@Override
	protected void onPause() {
		super.onPause();
		unbindService(conn);
		if (downloadController != null) {
			downloadController.setProgressNotifier(null);
		}
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		bindService(serviceIntent, conn, 0);
	}
	
}
