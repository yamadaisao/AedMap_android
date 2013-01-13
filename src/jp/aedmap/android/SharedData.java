package jp.aedmap.android;

import java.util.ArrayList;
import java.util.List;

import jp.aedmap.android.http.MarkerItem;
import jp.aedmap.android.http.MarkerItemResult;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

/**
 * Shared data between tabs.
 * 
 * @author yamada isao
 * 
 */
public class SharedData implements Parcelable {

	private static final String TAG = SharedData.class.getSimpleName();
	private static final boolean DEBUG = false;

	private static final SharedData instance = new SharedData();

	private CameraPosition cameraPosition;
	private MarkerItemResult lastResult;
	private boolean moveCurrent;
	private List<MarkerItem> editList;

	private SharedData() {
	}

	private SharedData(Parcel in) {
		LatLng target = new LatLng(in.readDouble(), in.readDouble());
		cameraPosition = new CameraPosition(target, in.readFloat(),
				in.readFloat(), in.readFloat());
		lastResult = in.readParcelable(null);
		moveCurrent = Boolean.valueOf(in.readString());
		in.readTypedList(editList, MarkerItem.CREATOR);
		if (DEBUG) {
			Log.v(TAG, "restore:result=" + lastResult.markers.size() + ",edit="
					+ editList.size());
		}
	}

	public static SharedData getInstance() {
		return instance;
	}

	public CameraPosition getGeoPoint() {
		return cameraPosition;
	}

	public void setGeoPoint(CameraPosition cameraPosition) {
		this.cameraPosition = cameraPosition;
	}

	public MarkerItemResult getLastResult() {
		return lastResult;
	}

	public void setLastResult(MarkerItemResult lastResult) {
		this.lastResult = lastResult;
	}

	public boolean isMoveCurrent() {
		return moveCurrent;
	}

	public void setMoveCurrent(boolean moveCurrent) {
		this.moveCurrent = moveCurrent;
	}

	public List<MarkerItem> getEditList() {
		if (editList == null) {
			editList = new ArrayList<MarkerItem>();
		}
		return editList;
	}

	public void setEditList(List<MarkerItem> editList) {
		this.editList = editList;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeDouble(cameraPosition.target.latitude);
		dest.writeDouble(cameraPosition.target.longitude);
		dest.writeFloat(cameraPosition.zoom);
		dest.writeFloat(cameraPosition.tilt);
		dest.writeFloat(cameraPosition.bearing);
		dest.writeParcelable(lastResult, 0);
		dest.writeString(Boolean.toString(moveCurrent));
		dest.writeTypedList(editList);
		if (DEBUG) {
			if (lastResult != null) {
				Log.v(TAG, "store:result=" + lastResult.markers.size()
						+ ",edit=" + editList.size());
			}
		}
	}

	public static final Parcelable.Creator<SharedData> CREATOR = new Parcelable.Creator<SharedData>() {
		@Override
		public SharedData createFromParcel(Parcel in) {
			return new SharedData(in);
		}

		@Override
		public SharedData[] newArray(int size) {
			return new SharedData[size];
		}
	};
}
