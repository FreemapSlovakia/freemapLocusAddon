package sk.freemap.locus.addon.routePlanner.mapDownloader;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

final class ProgressInputStream extends FilterInputStream {
	private long counter;
	private final ProgressListener progressListener;

	ProgressInputStream(final InputStream in, final ProgressListener progressListener) {
		super(in);
		this.progressListener = progressListener;
	}

	private int count(final int n) {
		if (n != -1) {
			counter += n;
		}
		progressListener.onProgress(counter);
		return n;
	}

	private long count(final long n) {
		if (n != -1) {
			counter += n;
		}
		progressListener.onProgress(counter);
		return n;
	}

	
	@Override
	public int read() throws IOException {
		return count(super.read());
	}


	@Override
	public int read(final byte[] buffer, final int offset, final int count) throws IOException {
		return count(super.read(buffer, offset, count));
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public long skip(final long byteCount) throws IOException {
		return count(super.skip(byteCount));
	}

	@Override
	public synchronized void reset() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized void mark(final int readlimit) {
		throw new UnsupportedOperationException();
	}
}