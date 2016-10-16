package sk.freemap.locus.addon.routePlanner.pointSelection;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import menion.android.locus.addon.publiclib.LocusIntents;
import menion.android.locus.addon.publiclib.geoData.Point;
import sk.freemap.locus.addon.routePlanner.R;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * @author martin
 *
 */
public class PointSelectionActivity extends Activity {
	
	private PointList pointList;
	private ArrayAdapter<PointWithImage> adapter;
	private final BlockingQueue<URL> bq = new LinkedBlockingQueue<URL>();
	private final Map<URL, Bitmap> bitmapCache = new ConcurrentHashMap<URL, Bitmap>();
	private Thread thread;
	private Button searchButton;
	private EditText nameEditText;
	private ListView pointsListView;
	private List<View> rowViews;
	private QueryPointsAsyncTask asyncTask;
	
	
	@Override
	protected void onResume() {
		super.onResume();
		
		thread = new Thread() {
			@Override
			public void run() {
				for (;;) {
					final URL url;
					try {
						url = bq.take();
					} catch (final InterruptedException e) {
						break;
					}
					
					final Bitmap bitmap;
					try {
						final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
						try {
							bitmap = BitmapFactory.decodeStream(connection.getInputStream());
					    } finally {
					    	connection.disconnect();
					    }
					} catch (final IOException e) {
						throw new RuntimeException(e);
					}

					bitmapCache.put(url, bitmap);
					for (final View row : rowViews) {
						final Object tag = row.getTag();
						if (url.equals(tag)) {
							final ImageView imageView = (ImageView) row.findViewById(R.id.imageView_pointImage);
							imageView.post(new Runnable() {
								@Override
								public void run() {
									imageView.setVisibility(View.VISIBLE);
									imageView.setImageBitmap(bitmap);
								}
							});
						}
					}
				}
			}
		};
		thread.start();
	}
	
	
	@Override
	protected void onPause() {
		super.onPause();
		thread.interrupt();
	}
	
	
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("list", pointList);
	}
	

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        final Object retained = getLastNonConfigurationInstance();
        if (retained instanceof QueryPointsAsyncTask) {
        	asyncTask = (QueryPointsAsyncTask) retained;
			asyncTask.setActivity(this);
        }
        
		setContentView(R.layout.point_selection);
		
		searchButton = (Button) findViewById(R.id.button_search);
		nameEditText = (((EditText) findViewById(R.id.editText_name)));
		pointsListView = (((ListView) findViewById(R.id.listView_points)));
		
		nameEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
			}
			
			@Override
			public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
			}
			
			@Override
			public void afterTextChanged(final Editable s) {
				searchButton.setEnabled(s.toString().length() > 0);
			}
		});
		
		final Object list = savedInstanceState == null ? null : savedInstanceState.getSerializable("list");
		pointList = list instanceof PointList ? (PointList) list : new PointList();
		
		rowViews = new CopyOnWriteArrayList<View>();
		
		adapter = new ArrayAdapter<PointWithImage>(this, R.layout.point_listitem, pointList) {
			@Override
			public View getView(final int position, final View convertView, final ViewGroup parent) {
				View row = convertView;
				
				if (row == null) {
					final LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					row = inflater.inflate(R.layout.point_listitem, parent, false);
					rowViews.add(row);
				}
				
				final PointWithImage pwi = getItem(position);
				
				final URL imageUrl = pwi.getImageUrl();
				row.setTag(imageUrl);
				
				((TextView) row.findViewById(R.id.textView_pointName)).setText(pwi.getPoint().getName());
				final ImageView imageView = (ImageView) row.findViewById(R.id.imageView_pointImage);
				
				if (imageUrl != null) {
					final Bitmap bitmap = bitmapCache.get(imageUrl);
					if (bitmap == null) {
						imageView.setVisibility(View.INVISIBLE);
						bq.add(imageUrl);
					} else {
						imageView.setImageBitmap(bitmap);
						imageView.setVisibility(View.VISIBLE);
					}
				} else {
					imageView.setVisibility(View.INVISIBLE);
				}
				return row;
			}
		};
		pointsListView.setAdapter(adapter);
		
		pointsListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
				onPointSelected(pointList.get(position));
			}
		});
		
		final Intent intent = getIntent();
		final Location location = (Location) intent.getParcelableExtra("locCenter");
		
		searchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				final StringBuilder urlSb = new StringBuilder("http://www.freemap.sk/api/0.1/q/");
				try {
					urlSb.append(URLEncoder.encode(nameEditText.getText().toString(), "UTF-8"));
				} catch (final UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
				if (location != null) {
					urlSb.append("?lat=").append(location.getLatitude());
					urlSb.append("?lon=").append(location.getLongitude());
				}
				
				final URL url;
				try {
					url = new URL(urlSb.toString());
				} catch (final MalformedURLException e) {
					throw new RuntimeException(e);
				}
				
				searchButton.setEnabled(false);
				nameEditText.setEnabled(false);
				adapter.clear();
				showDialog(0);
				asyncTask = new QueryPointsAsyncTask(PointSelectionActivity.this);
				asyncTask.execute(url);
			}
		});
	}
	
	
    @Override
    public Object onRetainNonConfigurationInstance() {
    	if (asyncTask != null) {
    		asyncTask.setActivity(null);
    	}
    	return asyncTask;
    }
    

	public void onTaskCompleted(final Object result) {
		if (result instanceof Exception) {
			Toast.makeText(this, ((Exception) result).getMessage(), Toast.LENGTH_LONG);
		} else if (result instanceof PointList) {
			pointList.addAll((PointList) result);
			adapter.notifyDataSetChanged();
		}
		asyncTask = null;
		searchButton.setEnabled(true);
		nameEditText.setEnabled(true);

// FIXME
//		java.lang.IllegalArgumentException: no dialog with id 0 was ever shown via Activity#showDialog
//			at android.app.Activity.missingDialog(Activity.java:2600)
//			at android.app.Activity.dismissDialog(Activity.java:2585)
		dismissDialog(0);
	}
	
	
    @Override
    protected Dialog onCreateDialog(final int id, final Bundle args) {
		switch (id) {
		case 0:
			final ProgressDialog progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("Searching points...");
			return progressDialog;
		}
		return null;
    }


	protected void onPointSelected(final PointWithImage pwi) {
		final Point point = pwi.getPoint();
		final Location location = point.getLocation();
		if (!LocusIntents.sendGetLocationData(PointSelectionActivity.this, point.getName(), location.getLatitude(), location.getLongitude(), 0.0, 0.0)) {
			Toast.makeText(PointSelectionActivity.this, "Wrong data to send!", Toast.LENGTH_SHORT).show();
		}
	}
	
	
	protected Bitmap getBitmap(final URL url) {
		return url == null ? null : bitmapCache.get(url);
	}

}
