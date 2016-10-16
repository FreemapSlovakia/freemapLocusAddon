package sk.freemap.locus.addon.routePlanner.mapDownloader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import sk.freemap.locus.addon.routePlanner.R;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

public class DownloadService extends Service {

	private Handler handler;
	private int pct = 0; // of the current file
	private ProgressNotifier progressNotifier;
	private boolean cancelled;
	private final DownloadControllerImpl downloadController = new DownloadControllerImpl();
	private FileInfo fileInfo; // currently downloaded
	private final Set<String> pendingIds = new CopyOnWriteArraySet<String>();

	@Override
	public void onCreate() {
		final LocalDatabase localDatabase = new LocalDatabase(this);
		
		final HandlerThread thread = new HandlerThread("ServiceStartArguments", android.os.Process.THREAD_PRIORITY_BACKGROUND);
	    thread.start();
	    
	    final Notification notification = new Notification(android.R.drawable.stat_sys_download, "Downloading", System.currentTimeMillis());
	    notification.contentView = new RemoteViews(getPackageName(), R.layout.progress_notif_area);
		notification.contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, DownloaderActivity.class), 0);
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		
	    handler = new Handler(thread.getLooper(), new Callback() {
			final File locusDir = Utils.findLocusDir(DownloadService.this);

			private void setProgress(final int percentage) {
				if (progressNotifier != null) {
					progressNotifier.onProgress(fileInfo.getId(), pct, pendingIds);
				}
				
				notification.contentView.setTextViewText(R.id.progressText, percentage + "% complete");
				notification.contentView.setProgressBar(R.id.progressBar, 100, percentage, false);
				startForeground(1, notification);
			}

			@Override
			public boolean handleMessage(final Message msg) {
				final Bundle bundle = msg.getData();
				fileInfo = bundle.getParcelable("fileInfo");
				
				if (!cancelled) {
					setProgress(0);
					
					try {
						final HttpURLConnection connection = (HttpURLConnection) new URL(bundle.getString("url")).openConnection();
						final long lenghtOfFile = connection.getContentLength();
						Log.d("ANDRO_ASYNC", "Lenght of file: " + lenghtOfFile);
						
						final InputStream is = new BufferedInputStream(new ProgressInputStream(connection.getInputStream(), new ProgressListener() {
							@Override
							public void onProgress(final long total) {
								final int percentage = (int) (total * 100L / lenghtOfFile);
								if (percentage != pct) {
									pct = percentage;
									
									setProgress(percentage);
								}
							}

						}));
						
						final File tmp = new File(locusDir.getAbsolutePath(), "freemap.tmp");
						delete(tmp);
						tmp.mkdir();
						
						final String destination = bundle.getString("destination");

						final List<String> paths = new ArrayList<String>();
						final ZipInputStream zis = new ZipInputStream(is);
						try {
							ZipEntry entry;
							while (!cancelled && (entry = zis.getNextEntry()) != null) {
								final String child = entry.getName().replace('/', File.separatorChar);
								paths.add(destination + File.separatorChar + child);
								final File file = new File(tmp, child);
								
								if (entry.isDirectory()) {
									if (!file.mkdir()) {
										throw new IOException("error creating directory");
									}
								} else {
									final FileOutputStream fos = new FileOutputStream(file);
									try {
										final byte[] buffer = new byte[1024];
										int n;
										while (!cancelled && (n = zis.read(buffer)) != -1) {
											fos.write(buffer, 0, n);
										}
									} finally {
										fos.close();
									}
								}
							}
						} finally {
							zis.close();
						}
						
						if (!cancelled) {
							final File destDir = new File(destination.replace("$LOCUS", locusDir.getAbsolutePath()));
							destDir.mkdirs();
							move(tmp, destDir);
							
							try {
								localDatabase.saveFileInfo(fileInfo, paths);
							} catch (final Exception e) {
								Log.w("DOWNLOAD", "Error saving file list.", e);
								Toast.makeText(DownloadService.this, "Error saving local database: " + e.getMessage(), Toast.LENGTH_LONG).show();
							}
						}
						
						delete(tmp);
					} catch (final Exception e) {
						Toast.makeText(DownloadService.this, "Download error: " + e.getMessage(), Toast.LENGTH_LONG).show();
					}
				}
				
				pendingIds.remove(fileInfo.getId());
				
				if (progressNotifier != null) {
					progressNotifier.onProgress(fileInfo.getId(), Integer.MAX_VALUE, pendingIds);
				}
				
				pct = -1;
				fileInfo = null;
				
				stopSelf(msg.arg1);
				
				return true;
			}
		});
	    
		startForeground(1, notification);
	}
	
	
	private static void delete(final File parent) {
		final File[] files = parent.listFiles();
		if (files != null) {
			for (final File file : files) {
				if (file.isDirectory()) {
					delete(file);
				}
				file.delete();
			}
		}
		parent.delete();
	}
	
	
	private static void move(final File src, final File dest) {
		if (dest.exists()) {
			if (dest.isFile()) {
				src.renameTo(dest);
			} else if (src.isDirectory()) {
				for (final File sub : src.listFiles()) {
					move(sub, new File(dest, sub.getName()));
				}
			} else {
				throw new RuntimeException("can't overwrite directory with file");
			}
		} else {
			src.renameTo(dest);
		}
	}
	
	
	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		if (intent == null) {
			return START_NOT_STICKY;
		}
		
		final Message message = handler.obtainMessage();
		message.arg1 = startId;
		message.setData(intent.getExtras());
		pendingIds.add(intent.getExtras().<FileInfo>getParcelable("fileInfo").getId());
		handler.sendMessage(message);
		return START_STICKY;
	}
	
	
    private final class DownloadControllerImpl extends Binder implements DownloadController {
		@Override
		public void setProgressNotifier(final ProgressNotifier progressNotifier) {
			DownloadService.this.progressNotifier = progressNotifier;
		}

		@Override
		public void cancel() {
			cancelled = true;
		}

		@Override
		public void askForCurrentProgressNow() {
			if (fileInfo != null) {
				progressNotifier.onProgress(fileInfo.getId(), pct, pendingIds);
			}
		}
	}

	
	@Override
	public IBinder onBind(final Intent intent) {
		return downloadController;
	}
	
	
	@Override
	public void onDestroy() {
		stopForeground(true);
	}

}
