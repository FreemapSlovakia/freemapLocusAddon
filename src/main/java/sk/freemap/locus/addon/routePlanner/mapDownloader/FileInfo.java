package sk.freemap.locus.addon.routePlanner.mapDownloader;

import android.os.Parcel;
import android.os.Parcelable;


public class FileInfo implements Parcelable {
	private final String id;
	private final long date;
	private final String version;
	private final String name;
	
	public static final Creator<FileInfo> CREATOR = new Creator<FileInfo>() {
        @Override
		public FileInfo createFromParcel(final Parcel in) {
            return new FileInfo(in);
        }
 
        @Override
		public FileInfo[] newArray(final int size) {
            return new FileInfo[size];
        }
    };
	
	public FileInfo(final Parcel in) {
		id = in.readString();
		date = in.readLong();
		version = in.readString();
		name = in.readString();
	}

	public FileInfo(final String id, final long date, final String version, final String name) {
		this.id = id;
		this.date = date;
		this.version = version;
		this.name = name;
	}
	
	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeString(id);
		dest.writeLong(date);
		dest.writeString(version);
		dest.writeString(name);
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	public String getId() {
		return id;
	}

	public long getDate() {
		return date;
	}

	public String getVersion() {
		return version;
	}

	public String getName() {
		return name;
	}
	
}