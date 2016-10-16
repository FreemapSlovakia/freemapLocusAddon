package sk.freemap.locus.addon.routePlanner;

import java.util.ArrayList;

import menion.android.locus.addon.publiclib.DisplayData;
import menion.android.locus.addon.publiclib.LocusConst;
import menion.android.locus.addon.publiclib.LocusIntents;
import menion.android.locus.addon.publiclib.LocusUtils;
import menion.android.locus.addon.publiclib.geoData.Point;
import menion.android.locus.addon.publiclib.utils.RequiredVersionMissingException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

public class FreemapRoutePlannerActivity extends Activity {

	private static final String TAG = "RP";
	private static final String[] TYPES = new String[] { "motorcar", "bicycle", "foot", "hiking" };

	/*
	 * Useful TIPS:
	 *
	 * * check LocusUtils class, that contain some interesting functions, mainly
	 *  - ability to check if is Locus installed (thanks to Arcao)
	 *  - ability to send some file from fileSystem to Locus (for import)
	 *
	 * * check LocusIntents class, that contain main info how to integrate some
	 * call-backs from Locus application
	 *
	 * * check DisplayData class, that contain all functions required for sending
	 * various data into Locus. Samples contain mainly calling of this function
	 *
	 * * if you miss some function, or you wrote something simple and nice that should
	 * come handy to others, let me know on locus@asamm.cz and I'll add it to this API
	 */
	
	private static class PointItem {
		Point point;
		
		@Override
		public String toString() {
			return point.getName();
//			return String.format("%s (%4f %4f)", point.getName(), point.getLocation().getLatitude(), point.getLocation().getLongitude());
		}
	}

	private final ArrayList<PointItem> pointItemList = new ArrayList<PointItem>();
	private ArrayAdapter<PointItem> adapter;
	private Button calculateButton;
	private Button clearButton;
	private CheckBox saveCheckBox;
	private CheckBox fastestCheckBox;
	private Spinner routeTypeSpinner;
	private QueryAsyncTask asyncTask;


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Object retained = getLastNonConfigurationInstance();
        if (retained instanceof QueryAsyncTask) {
        	asyncTask = (QueryAsyncTask) retained;
			asyncTask.setActivity(this);
        }
        
        setContentView(R.layout.main);
        
		saveCheckBox = (CheckBox) findViewById(R.id.checkBox_save);
		fastestCheckBox = (CheckBox) findViewById(R.id.checkBox_fastest);
		routeTypeSpinner = (Spinner) findViewById(R.id.spinner_routeType);
        clearButton = (Button) findViewById(R.id.btn_clear);
        calculateButton = (Button) findViewById(R.id.btn_calculate);
        adapter = new ArrayAdapter<PointItem>(this, android.R.layout.simple_list_item_1, pointItemList);
        
		((ListView) findViewById(R.id.points)).setAdapter(adapter);

        if (savedInstanceState != null) {
	        final ArrayList<Parcelable> pointList = savedInstanceState.getParcelableArrayList("points");
	        if (pointList != null) {
	        	for (final Parcelable p : pointList) {
	        		addPoint((Point) p);
	        	}
	        }
        }
        
        ((Button) findViewById(R.id.btn_addAnotherPoint)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (LocusUtils.isLocusAvailable(FreemapRoutePlannerActivity.this, 63, 126)) {
					final Intent intent = new Intent(LocusConst.ACTION_PICK_LOCATION);
					startActivityForResult(intent, 666);
				}
			}
		});
        
        clearButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				showDialog(1);
			}
		});
        
		calculateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				final StringBuilder sb = new StringBuilder("http://www.freemap.sk/api/0.1/r/");
				boolean first = true;
				for (final PointItem pointItem : pointItemList) {
					if (first) {
						first = false;
					} else {
						sb.append('/');
					}
					sb.append(pointItem.point.getLocation().getLatitude()).append("%7C").append(pointItem.point.getLocation().getLongitude());
				}
				
				sb.append('/');
				sb.append(TYPES[routeTypeSpinner.getSelectedItemPosition()]);
				if (fastestCheckBox.isChecked()) {
					sb.append("/fastest");
				}
//				sb.append("&Format=GPX");
				
				Log.d(TAG, "URL: " + sb.toString());

				showDialog(0);
				
				asyncTask = new QueryAsyncTask(FreemapRoutePlannerActivity.this);
				asyncTask.execute(sb.toString(), saveCheckBox.isChecked());
			}
		});
        
        if (LocusUtils.isLocusAvailable(this, 64, 126)) {
        	handleIntent(getIntent());
        } else {
        	showDialog(2);
        }
    }
    
	@Override
    protected Dialog onCreateDialog(final int id, final Bundle args) {
		switch (id) {
		case 0:
			final ProgressDialog progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("Computing final the track...");
			return progressDialog;
		case 1:
			return new AlertDialog.Builder(FreemapRoutePlannerActivity.this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle("Delete all points")
					.setMessage("Are you sure to clear all points?")
					.setPositiveButton("Yes", new Dialog.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							clearPoints();
						}
					}).setNegativeButton("No", null).create();
		case 2:
        	return new AlertDialog.Builder(this)
					.setTitle("Locus not found")
					.setMessage("Required version of Locus is not installed on this device. Install?")
					.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=menion.android.locus")));
						}
					})
					.setNegativeButton("No", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							System.exit(0);
						}
					})
					.create();
		case 3:
			return new AlertDialog.Builder(this)
					.setMessage("ERROR: " + args.getString("errorMessage"))
					.setPositiveButton("OK", null)
					.create();
		}
		return null;
    }
    
    
	private void clearPoints() {
		pointItemList.clear();
		adapter.notifyDataSetChanged();
		calculateButton.setEnabled(false);
		clearButton.setEnabled(false);
//		((ListView) findViewById(R.id.points)).removeAllViews();
	}
	

	private void handleIntent(final Intent intent) {
		if (intent == null || intent.getBooleanExtra("used", false)) {
			return;
		}
		
		intent.putExtra("used", true);

        if (LocusIntents.isIntentOnPointAction(intent)) {
        	final Point p = LocusIntents.handleIntentOnPointAction(intent);
        	if (p == null) {
        		Toast.makeText(FreemapRoutePlannerActivity.this, "Wrong INTENT - no point!", Toast.LENGTH_SHORT).show();
        	} else {
        		addPoint(p);
        	}
    	} else if (LocusIntents.isIntentReceiveLocation(intent)) {
			final Point p = LocusIntents.handleActionReceiveLocation(intent);
			if (p == null) {
				Log.w(TAG, "request PickLocation, cancelled");
			} else {
				addPoint(p);
			}
        }
	}
	
	
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		final ArrayList<Point> pointList = new ArrayList<Point>();
		for (final PointItem pointItem : pointItemList) {
			pointList.add(pointItem.point);
		}
		outState.putParcelableArrayList("points", pointList);
	}
	

	private void addPoint(final Point p) {
		final PointItem pi = new PointItem();
		pi.point = p;
		pointItemList.add(pi);
		calculateButton.setEnabled(pointItemList.size() > 1);
		clearButton.setEnabled(pointItemList.size() > 0);
		adapter.notifyDataSetChanged();
	}
	
	
	@Override
	protected void onPause() {
		super.onPause();
		
		final Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		editor.putBoolean("fastest", fastestCheckBox.isChecked());
		editor.putBoolean("save", saveCheckBox.isChecked());
		editor.putInt("routeType", routeTypeSpinner.getSelectedItemPosition());
		editor.commit();
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		final boolean fastest = pref.getBoolean("fastest", true); // split for tracking NPE
		fastestCheckBox.setChecked(fastest);
		saveCheckBox.setChecked(pref.getBoolean("save", false));
		routeTypeSpinner.setSelection(pref.getInt("routeType", 0));
	}
	
    
    @Override
    protected void onNewIntent(final Intent intent) {
    	super.onNewIntent(intent);
    	handleIntent(intent);
    }
    
    
    @Override
    public Object onRetainNonConfigurationInstance() {
    	if (asyncTask != null) {
    		asyncTask.setActivity(null);
    	}
    	return asyncTask;
    }
    

	public void onTaskCompleted(final Result result) {
		try {
			dismissDialog(0);
		} catch (final IllegalArgumentException e) {
			// ignore
		}
		if (result.errorMessage == null) {
			try {
				DisplayData.sendData(this, result.track, result.imp);
			} catch (final RequiredVersionMissingException e) {
				Log.e(TAG, "DisplayData.sendData", e);
			}
		} else {
			final Bundle bundle = new Bundle();
			bundle.putString("errorMessage", result.errorMessage);
			showDialog(3, bundle);
		}
		asyncTask = null;
	}
    
}