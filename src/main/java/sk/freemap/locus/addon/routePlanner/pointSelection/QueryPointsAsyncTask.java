package sk.freemap.locus.addon.routePlanner.pointSelection;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import menion.android.locus.addon.publiclib.geoData.Point;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.os.AsyncTask;

class QueryPointsAsyncTask extends AsyncTask<URL, Void, Object> {
	
	private PointSelectionActivity activity;
	private Object result;
	
	
	QueryPointsAsyncTask(final PointSelectionActivity activity) {
		this.activity = activity;
	}
	

	@Override
	protected Object doInBackground(final URL... params) {
		final StringBuffer sb = new StringBuffer();
		
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) params[0].openConnection();
			final Reader reader = new InputStreamReader(connection.getInputStream());
			
			final char[] chars = new char[1024];
			int n;
			while ((n = reader.read(chars)) != -1) {
				sb.append(chars, 0, n);
			}
		} catch (final IOException e) {
			return new RuntimeException("Error communicating with server", e);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		
		final PointList pointList = new PointList();
		
		try {
			final JSONArray ja = new JSONArray(sb.toString());
			for (int i = 0, len = ja.length(); i < len; i++) {
				final JSONObject jo = ja.getJSONObject(i);
				final Location loc = new Location("");
				loc.setLatitude(Double.parseDouble(jo.getString("lat")));
				loc.setLongitude(Double.parseDouble(jo.getString("lon")));
				pointList.add(new PointWithImage(new Point(jo.getString("name"), loc),
						jo.has("icon") ? URI.create("http://www.freemap.sk/" + jo.getString("icon")).toURL() : null));
			}
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e);
		} catch (final JSONException e) {
			throw new RuntimeException(e);
		}
		
		return pointList;
	}
	
	
	@Override
	protected void onPostExecute(final Object result) {
		this.result = result;
		notifyActivityTaskCompleted();
	}
	
	
	public void setActivity(final PointSelectionActivity activity) {
		this.activity = activity;
		if (result != null) {
			notifyActivityTaskCompleted();
		}
	}
	
	
	private void notifyActivityTaskCompleted() {
		if (activity != null) {
			activity.onTaskCompleted(result);
		}
	}

}
