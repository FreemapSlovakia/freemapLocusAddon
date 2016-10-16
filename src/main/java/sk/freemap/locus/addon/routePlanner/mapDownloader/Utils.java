package sk.freemap.locus.addon.routePlanner.mapDownloader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import menion.android.locus.addon.publiclib.LocusIntents;
import menion.android.locus.addon.publiclib.utils.RequiredVersionMissingException;
import android.content.Context;
import android.os.Environment;

public final class Utils {
	
	private Utils() {
		throw new AssertionError();
	}
	

	public static String streamToString(final InputStream is) throws IOException {
		final Reader reader = new InputStreamReader(new BufferedInputStream(is));
		final StringBuilder sb = new StringBuilder();
		final char[] buf = new char[1024];
		int n;
		while ((n = reader.read(buf)) != -1) {
			sb.append(buf, 0, n);
		}
		return sb.toString();
	}
	
	
	public static File findLocusDir(final Context context) {
		File dir = null;
		try {
			final String locusRootDirectory = LocusIntents.getLocusRootDirectory(context);
			if (locusRootDirectory != null) {
				dir = new File(locusRootDirectory);
			}
		} catch (final RequiredVersionMissingException e) {
			// ignore
		}

		// fallback
		if (dir == null) {
			final File externalStorageDirectory = Environment.getExternalStorageDirectory();
			dir = new File(externalStorageDirectory, "Locus");
			if (dir.isDirectory()) {
				return dir;
			}
			dir = new File(externalStorageDirectory, "external_sd" + File.separatorChar + "Locus");
			if (dir.isDirectory()) {
				return dir;
			}
		}
		return dir;
	}
	
}
