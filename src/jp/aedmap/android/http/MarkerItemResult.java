package jp.aedmap.android.http;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

public class MarkerItemResult implements Parcelable {

	public double queryLatitude;
	public double queryLongitude;
	public double minLatitude;
	public double minLongitude;
	public double maxLatitude;
	public double maxLongitude;
	public MarkerItem targetMarker;
	public List<MarkerItem> markers = new ArrayList<MarkerItem>();

	public MarkerItemResult(LatLng latlng) {
		queryLatitude = latlng.latitude;
		queryLongitude = latlng.longitude;
		minLatitude = queryLatitude;
		minLongitude = queryLongitude;
		maxLatitude = queryLatitude;
		maxLongitude = queryLongitude;
	}

	private MarkerItemResult(Parcel in) {
		queryLatitude = in.readDouble();
		queryLongitude = in.readDouble();
		minLatitude = in.readDouble();
		minLongitude = in.readDouble();
		maxLatitude = in.readDouble();
		maxLongitude = in.readDouble();
		targetMarker = in.readParcelable(MarkerItem.class.getClassLoader());
		markers = in.createTypedArrayList(MarkerItem.CREATOR);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeDouble(queryLatitude);
		dest.writeDouble(queryLongitude);
		dest.writeDouble(minLatitude);
		dest.writeDouble(minLongitude);
		dest.writeDouble(maxLatitude);
		dest.writeDouble(maxLongitude);
		dest.writeParcelable(targetMarker, flags);
		dest.writeTypedList(markers);
	}

	public static final Parcelable.Creator<MarkerItemResult> CREATOR = new Parcelable.Creator<MarkerItemResult>() {
		@Override
		public MarkerItemResult createFromParcel(Parcel in) {
			return new MarkerItemResult(in);
		}

		@Override
		public MarkerItemResult[] newArray(int size) {
			return new MarkerItemResult[size];
		}
	};
}
