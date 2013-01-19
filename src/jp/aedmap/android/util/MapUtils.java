package jp.aedmap.android.util;

import java.util.Comparator;

import jp.aedmap.android.R;
import jp.aedmap.android.http.MarkerItem;
import android.content.Context;
import android.location.Location;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapUtils {

	public static final float calcZoom(float accuracy) {
		float zoomLevel;
		if (accuracy < 100) {
			zoomLevel = 20f;
		} else if (accuracy < 200) {
			zoomLevel = 19f;
		} else if (accuracy < 400) {
			zoomLevel = 18f;
		} else if (accuracy < 800) {
			zoomLevel = 17f;
		} else if (accuracy < 1600) {
			zoomLevel = 16f;
		} else if (accuracy < 3200) {
			zoomLevel = 15f;
		} else if (accuracy < 6400) {
			zoomLevel = 14f;
		} else if (accuracy < 12800) {
			zoomLevel = 13f;
		} else if (accuracy < 25600) {
			zoomLevel = 12f;
		} else if (accuracy < 51200) {
			zoomLevel = 11f;
		} else if (accuracy < 102400) {
			zoomLevel = 10f;
		} else {
			zoomLevel = 9f;
		}
		return zoomLevel;
	}

	public static float calculateZoomLevel(Context ctx, float accuracy) {
		double equatorLength = 40075004; // in meters
		WindowManager wm = (WindowManager) ctx
				.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics metrics = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(metrics);
		double widthInPixels = metrics.widthPixels / metrics.density;
		double metersPerPixel = equatorLength / 256 / metrics.density;
		// 68 is percent
		double diameter = (accuracy / 68 * 100) * 2;
		int zoomLevel = 1;
		while ((metersPerPixel * widthInPixels) > diameter) {
			metersPerPixel /= 2;
			++zoomLevel;
		}
		return zoomLevel;
	}

	private static BitmapDescriptor ICON_AED = BitmapDescriptorFactory
			.fromResource(R.drawable.ic_aed);

	public static final MarkerOptions createOptions(MarkerItem item) {
		MarkerOptions options = new MarkerOptions();
		options.position(item.position);
		options.title(item.name);
		options.snippet(item.adr);
		options.icon(ICON_AED);
		return options;
	}

	/**
	 * 2転換の距離をメートル単位で算出します.
	 * <p>
	 * 誤差があるとの話もありますが、アプリケーションの緊急度合を考えると大きな誤差が発生する大陸間の距離を測りたいわけではないので、
	 * この方法をとることにします.
	 * </p>
	 * 
	 * @param from
	 *            2点間の始点
	 * @param to
	 *            2点間の終点
	 * @return 2点間の距離メートル単位)
	 */
	public static long getDistance(LatLng from, LatLng to) {
		double fromLat = from.latitude;
		double fromLng = from.longitude;
		double toLat = to.latitude;
		double toLng = to.longitude;
		float[] result = { 0, 0, 0 };
		Location.distanceBetween(fromLat, fromLng, toLat, toLng, result);
		return (long) result[0];
	}

	/**
	 * MarkerItemの比較クラス.
	 * 
	 * @author yamadaisao
	 * 
	 */
	public static class MarkerComparator implements Comparator<MarkerItem> {
		@Override
		public int compare(MarkerItem o1, MarkerItem o2) {
			return (int) (o1.dist - o2.dist);
		}
	}
}
