package sk.freemap.locus.addon.routePlanner.mapDownloader;

import java.util.Set;

interface ProgressNotifier {

	void onProgress(String id, int pct, Set<String> pendingIds);
	
}
