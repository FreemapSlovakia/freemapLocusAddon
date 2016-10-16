package sk.freemap.locus.addon.routePlanner.mapDownloader;

import java.io.IOException;

public interface DownloadResultHandler {

	void handleResult(String result);

	void handleException(IOException e);

}
