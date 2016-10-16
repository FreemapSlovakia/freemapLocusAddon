package sk.freemap.locus.addon.routePlanner.mapDownloader;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;

import sk.freemap.locus.addon.routePlanner.R;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class DownloaderActivity extends Activity {

	private boolean initialized;

	private Intent serviceIntent;
	
	private DownloadController downloadController;
	private Map<String, FileInfo> serverFileInfoMap;
	private LocalDatabase localDatabase;
	
	private final Map<String, State> stateMap = new HashMap<String, State>();
	
	private final ServiceConnection conn = new ServiceConnection() {
		@Override
		public void onServiceConnected(final ComponentName name, final IBinder binder) {
			cancelButton.post(new Runnable() {
				@Override
				public void run() {
					cancelButton.setVisibility(Button.VISIBLE);
				}
			});
			
			downloadController = (DownloadController) binder;
			downloadController.setProgressNotifier(new ProgressNotifier() {
				@Override
				public void onProgress(final String id, final int pct, final Set<String> pendingIds) {
					if (initialized) {
						DownloaderActivity.this.runOnUiThread(new Runnable() {
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
			DownloaderActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					cancelButton.setVisibility(Button.GONE);
					drawGui();
				}
			});
			
			downloadController.setProgressNotifier(null);
			downloadController = null;
			bindService(serviceIntent, conn, 0); // rebind, because disconnection unbinds
		}
	};
	
	private ListView listView;
	private ArrayAdapter<FileInfo> adapter;
	private ProgressBar progressBar;
	private Button cancelButton;


	@Override
	public Object onRetainNonConfigurationInstance() {
		return serverFileInfoMap;
	}
	

	private void drawGui() {
		adapter.clear();
		
		try {
			for (final FileInfo fileInfo : serverFileInfoMap.values()) {
				final FileInfo localFileInfo = localDatabase.getFileInfo(fileInfo.getId());
				stateMap.put(fileInfo.getId(), localFileInfo == null || localFileInfo.getDate() == 0 ? State.AVAILABLE : localFileInfo.getDate() >= fileInfo.getDate() ? State.UP_TO_DATE : State.OUTDATED);
				adapter.add(fileInfo);
			}

			for (final FileInfo fileInfo : localDatabase.getFileInfos()) {
				if (fileInfo.getDate() != 0 && !serverFileInfoMap.containsKey(fileInfo.getId())) {
					stateMap.put(fileInfo.getId(), State.OBSOLETE);
					adapter.add(fileInfo);
				}
			}
		} catch (final IOException e) {
			Toast.makeText(DownloaderActivity.this, "Error reading local database.", Toast.LENGTH_LONG).show();
		}
	}


	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		serviceIntent = new Intent(this, DownloadService.class);
		
		setContentView(R.layout.download);
		
		listView = (ListView) findViewById(R.id.listView_items);
		
		progressBar = (ProgressBar) findViewById(R.id.progressBar_loadList);
		
		cancelButton = (Button) findViewById(R.id.button_cancel);
		
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (downloadController != null) {
					downloadController.cancel();
				}
			}
		});
		
		localDatabase = new LocalDatabase(this);
		
		adapter = new ArrayAdapter<FileInfo>(this, R.layout.list_item, new ArrayList<FileInfo>()) {
			@Override
			public View getView(final int position, final View convertView, final ViewGroup parent) {
				View row = convertView;
				
				if (row == null) {
					final LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					row = inflater.inflate(R.layout.list_item, parent, false);
				}
				
				final TextView textView = (TextView) row.findViewById(R.id.textView_resName);
				final FileInfo fileInfo = getItem(position);
				textView.setText(fileInfo.getName());
				
				textView.setTextColor(stateMap.get(fileInfo.getId()).getColor());
				
				return row;
			}
		};
		
		listView.setAdapter(adapter);
		
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> adapterView, final View view, final int position, final long id) {
				final Intent intent = new Intent(DownloaderActivity.this, DetailActivity.class);
				final String id2 = adapter.getItem(position).getId();
				intent.putExtra("id", id2);
//				final FileInfo fileInfo = serverFileInfoMap.get(id2);
//				if (fileInfo != null) {
//					intent.putExtra("serverDate", fileInfo.getDate());
//				}
				startActivity(intent);
			}
		});
		
        final Object retained = getLastNonConfigurationInstance();
        if (retained != null) {
        	serverFileInfoMap = (Map<String, FileInfo>) retained;
			initGui();
        } else {
			new DownloadAsyncTask("http://proxy.freemap.sk/locus/2/resources", new DownloadResultHandler() {
				@Override
				public void handleResult(final String result) {
					try {
						serverFileInfoMap = LocalDatabase.createFileInfoMap(result);
						initGui();
					} catch (final JSONException e) {
						Toast.makeText(DownloaderActivity.this, "Error parsing response from server.", Toast.LENGTH_LONG).show();
					}
				}

				@Override
				public void handleException(final IOException e) {
					Toast.makeText(DownloaderActivity.this, "Error in communication with server: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
	

	private void initGui() {
		drawGui();
		
		progressBar.setVisibility(ProgressBar.GONE);
		
		initialized = true;
		
		if (downloadController != null) {
			downloadController.askForCurrentProgressNow();
		}
	}

	
	@Override
	protected void onResume() {
		super.onResume();
		if (serverFileInfoMap != null) {
			drawGui();
		}
		bindService(serviceIntent, conn, 0);
	}
	
	
	@Override
	protected void onPause() {
		super.onPause();
		unbindService(conn);
		if (downloadController != null) {
			downloadController.setProgressNotifier(null);
		}
	}

}
