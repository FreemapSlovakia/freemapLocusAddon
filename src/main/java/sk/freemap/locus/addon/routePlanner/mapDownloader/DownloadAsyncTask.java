package sk.freemap.locus.addon.routePlanner.mapDownloader;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import android.os.AsyncTask;

public class DownloadAsyncTask extends AsyncTask<Void, Void, Object> {

	private final DownloadResultHandler downloadResultHandler;
	private final String url;

	
	public DownloadAsyncTask(final String url, final DownloadResultHandler downloadResultHandler) {
		this.url = url;
		this.downloadResultHandler = downloadResultHandler;
	}
	

	@Override
	protected final Object doInBackground(final Void... params) {
		try {
			final HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
			
			setupCnnection(con);
			
			try {
				return Utils.streamToString(con.getInputStream());
			} finally {
				con.disconnect();
			}
		} catch (final IOException e) {
			return e;
		}
	}
	

	protected void setupCnnection(final HttpURLConnection con) {
		final StringBuilder sb = new StringBuilder();
		final Locale locale = Locale.getDefault();
		final String language = locale.getLanguage();
		if (language.length() > 0) {
			sb.append(language);
			final String country = locale.getCountry();
			if (country.length() > 0) {
				sb.append('-').append(country);
			}
			con.setRequestProperty("Accept-Language", sb.toString());
		}
	}


	@Override
	protected final void onPostExecute(final Object result) {
		if (result instanceof IOException) {
			downloadResultHandler.handleException((IOException) result);
		} else {
			this.downloadResultHandler.handleResult((String) result);
		}
	}
	
}