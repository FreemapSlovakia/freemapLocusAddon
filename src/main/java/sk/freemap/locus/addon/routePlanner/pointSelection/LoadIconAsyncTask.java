package sk.freemap.locus.addon.routePlanner.pointSelection;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

/**
 * 
 * @author martin
 *
 */
class LoadIconAsyncTask extends AsyncTask<Object, Void, Object[]> {
	
	@Override
	protected Object[] doInBackground(final Object... params) {
		try {
			final HttpURLConnection connection = (HttpURLConnection) ((URL) params[1]).openConnection();
			try {
				return new Object[] { params[0], BitmapFactory.decodeStream(connection.getInputStream()) };
		    } finally {
		    	connection.disconnect();
		    }
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
	

	@Override
	protected void onPostExecute(final Object[] result) {
		((ImageView) result[0]).setImageBitmap((Bitmap) result[1]);
	}
	
}