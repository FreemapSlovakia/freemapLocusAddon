package sk.freemap.locus.addon.routePlanner.pointSelection;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

class PointList extends ArrayList<PointWithImage> implements Parcelable {

	private static final long serialVersionUID = -8886260970240217597L;

	public PointList(final Parcel in) {
		final int count = in.readInt();
		for (int i = 0; i < count; i++) {
			add((PointWithImage) in.readValue(PointWithImage.class.getClassLoader()));
		}
	}
	
	public PointList() {
		super();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeInt(size());
		for (final PointWithImage point : this) {
			dest.writeValue(point);
		}
	}
	
	public static final Creator<PointList> CREATOR = new Creator<PointList>() {
        @Override
		public PointList createFromParcel(final Parcel in) {
            return new PointList(in);
        }

        @Override
		public PointList[] newArray(final int size) {
            return new PointList[size];
        }
    };
	
}