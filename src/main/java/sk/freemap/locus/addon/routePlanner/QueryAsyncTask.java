package sk.freemap.locus.addon.routePlanner;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

import menion.android.locus.addon.publiclib.geoData.Track;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;
import android.util.Xml.Encoding;

class Result {
	boolean imp;
	Track track;
	String errorMessage;
}

class QueryAsyncTask extends AsyncTask<Object, Void, Result> {

    	private static final String LINESTRING = "LINESTRING(";
    	private static final String TAG = "QueryAsyncTask";

		private FreemapRoutePlannerActivity activity;
		private Result result;
    	
    	QueryAsyncTask(final FreemapRoutePlannerActivity activity) {
    		this.activity = activity;
		}

//		@Override
//    	protected void onPreExecute() {
//			activity.showDialog(id);
////			dialog = ProgressDialog.show(activity, "", "Computing the track...", true);
//    	}
    	
		@Override
		protected Result doInBackground(final Object... params) {
    		final Result result = new Result();
    		result.imp = (Boolean) params[1];
    		final Track track = new Track();
    		
    		HttpURLConnection connection = null;
    		try {
    			connection = (HttpURLConnection) URI.create((String) params[0]).toURL().openConnection();
				track.setName("Freemap Route");
				track.setStyle(Color.MAGENTA, 5.0f);

    			final InputStream is = connection.getInputStream();
    			
				try {
					final String contentType = connection.getContentType();
					if (connection.getResponseCode() == 200) {
    					Xml.parse(is, Encoding.UTF_8, new DefaultHandler() {
    						boolean inWkt;
    						final StringBuffer sb = new StringBuffer();
    						
    						@Override
    						public void startElement(final String uri, final String localName, final String qName, final org.xml.sax.Attributes attributes) throws SAXException {
    							if ("wkt".equals(localName)) {
    								inWkt = true;
    							}
    						};
    						
    						@Override
    						public void characters(final char[] ch, final int start, final int length) throws SAXException {
    							if (inWkt) {
    								sb.append(ch, start, length);
    							}
    						};
    						
    						@Override
    						public void endElement(final String uri, final String localName, final String qName) throws SAXException {
    							if ("wkt".equals(localName)) {
    								inWkt = false;
    								final int from = sb.indexOf(LINESTRING);
    								final int to = sb.indexOf(")", from + LINESTRING.length());
    								for (final String ll : sb.substring(from + LINESTRING.length(), to).split(", ")) {
    									final int n = ll.indexOf(' ');
    									if (n != -1) {
	    									final Location loc = new Location(TAG);
	    									loc.setLatitude(Double.parseDouble(ll.substring(n + 1))); // NumberFormatException reported
	    									loc.setLongitude(Double.parseDouble(ll.substring(0, n)));
	    									track.addLocation(loc);
    									}
    								}
    							}
    						};
    					});
    					
//					Xml.parse(is, Encoding.UTF_8, new DefaultHandler() {
//						@Override
//						public void startElement(final String uri, final String localName, final String qName, final org.xml.sax.Attributes attributes) throws SAXException {
//							if ("trkpt".equals(localName)) {
//								final Location loc = new Location(TAG);
//								loc.setLatitude(Double.parseDouble(attributes.getValue("lat")));
//								loc.setLongitude(Double.parseDouble(attributes.getValue("lon")));
//								track.addLocation(loc);
//							}
//						};
//					});
    					if (track.getLocations() == null) {
    						result.errorMessage = "No route found. Verify if all points are in Slovakia.";
    					} else {
    						result.track = track;
    					}
					} else {
						final StringBuilder sb = new StringBuilder();
						final InputStreamReader reader = new InputStreamReader(is, "UTF-8");
						int n;
						final char[] buf = new char[1024];
						while ((n = reader.read(buf)) != -1) {
							sb.append(buf, 0, n);
						}
						result.errorMessage = "Server response: " + sb.toString();
					}
				} catch (final SAXException e) {
					Log.e(TAG, e.getMessage(), e);
					result.errorMessage = "Error parsing XML from server: " + e.getMessage();
				} finally {
					is.close();
				}
			} catch (final IOException e) {
				Log.e(TAG, e.getMessage(), e);
				result.errorMessage = "Communication error with the server: " + e.getMessage();
			} finally {
				if (connection != null) {
					connection.disconnect();
				}
			}

    		return result;
		}
		
		@Override
		protected void onPostExecute(final Result result) {
			this.result = result;
			notifyActivityTaskCompleted();
		}
		
		
		public void setActivity(final FreemapRoutePlannerActivity activity) {
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