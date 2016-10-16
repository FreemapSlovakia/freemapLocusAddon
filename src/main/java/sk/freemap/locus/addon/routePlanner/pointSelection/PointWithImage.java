package sk.freemap.locus.addon.routePlanner.pointSelection;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

import menion.android.locus.addon.publiclib.geoData.Point;
import android.os.Parcel;
import android.os.Parcelable;

public class PointWithImage implements Serializable, Parcelable {
	private static final long serialVersionUID = 3310994717134526520L;
	
	private final Point point;
	private final URL imageUrl;
	
	public PointWithImage(final Parcel in) {
		point = in.readParcelable(getClass().getClassLoader());
		try {
			final String u = in.readString();
			imageUrl = u == null ? null : new URL(u);
		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public PointWithImage(final Point point, final URL imageUrl) {
		this.point = point;
		this.imageUrl = imageUrl;
	}

	@Override
	public String toString() {
		return point.getName();
	}

	@Override
	public int describeContents() {
		return 0;
	}
	
	public static final Creator<PointWithImage> CREATOR = new Creator<PointWithImage>() {
        @Override
		public PointWithImage createFromParcel(final Parcel in) {
            return new PointWithImage(in);
        }
 
        @Override
		public PointWithImage[] newArray(final int size) {
            return new PointWithImage[size];
        }
    };

	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeParcelable(point, 0);
		dest.writeString(imageUrl == null ? null : imageUrl.toString());
	}

	
	public Point getPoint() {
		return point;
	}
	

	public URL getImageUrl() {
		return imageUrl;
	}
	
}