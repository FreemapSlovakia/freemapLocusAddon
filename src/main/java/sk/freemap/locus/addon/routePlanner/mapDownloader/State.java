package sk.freemap.locus.addon.routePlanner.mapDownloader;

import android.graphics.Color;

enum State {
	AVAILABLE(Color.LTGRAY),
	OUTDATED(Color.rgb(255, 128, 0)),
	UP_TO_DATE(Color.GREEN),
	OBSOLETE(Color.RED);
	
	private final int color;

	private State(final int color) {
		this.color = color;
	}
	
	public int getColor() {
		return color;
	}
}