package sk.freemap.locus.addon.routePlanner;

import sk.freemap.locus.addon.routePlanner.mapDownloader.BootBroadcastReceiver;
import sk.freemap.locus.addon.routePlanner.mapDownloader.DownloaderActivity;
import sk.freemap.locus.addon.routePlanner.mapDownloader.Utils;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainMenuActivity extends Activity {

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.menu);
		
		BootBroadcastReceiver.scheduleUpdateCheck(this);
		
		((Button) findViewById(R.id.button_routePlanner)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				startActivity(new Intent(MainMenuActivity.this, FreemapRoutePlannerActivity.class));
			}
		});
		
		((Button) findViewById(R.id.button_poiSearch)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				startActivity(new Intent(MainMenuActivity.this, PoiSearchActivity.class));
			}
		});
		
		((Button) findViewById(R.id.button_mapDownloader)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (Utils.findLocusDir(MainMenuActivity.this) == null) {
					new AlertDialog.Builder(MainMenuActivity.this)
							.setMessage("Sorry, but no Locus directory has been found.")
							.setPositiveButton("OK", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(final DialogInterface dialog, final int which) {
									dialog.dismiss();
								}
							}
					).create().show();
				} else {
					startActivity(new Intent(MainMenuActivity.this, DownloaderActivity.class));
				}
			}
		});
		
//		if (getSharedPreferences("c2dm", Context.MODE_PRIVATE).contains(C2DmConstants.REGISTRATION_ID)) {
//			c2dmRegister();
//		}
	}
	
	
//	private void c2dmRegister() {
//		final Intent registrationIntent = new Intent(C2DmConstants.ACTION_REGISTER);
//		registrationIntent.putExtra(C2DmConstants.EXTRA_APP, PendingIntent.getBroadcast(this, 0, new Intent(), 0));
//		registrationIntent.putExtra(C2DmConstants.EXTRA_SENDER, "freemap.locus.addon@gmail.com");
//		startService(registrationIntent);
//	}
	
}
