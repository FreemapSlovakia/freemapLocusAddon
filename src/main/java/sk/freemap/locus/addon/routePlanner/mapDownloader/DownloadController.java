package sk.freemap.locus.addon.routePlanner.mapDownloader;

interface DownloadController {

	void setProgressNotifier(ProgressNotifier progressNotifier);

	void cancel();

	void askForCurrentProgressNow();

}
