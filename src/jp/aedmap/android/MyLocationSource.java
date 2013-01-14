package jp.aedmap.android;

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.LocationSource;

/**
 * 
 * @author yamada.isao
 * 
 */
public class MyLocationSource implements LocationSource {

	private static final boolean DEBUG = true;
	private static final String TAG = MyLocationSource.class.getSimpleName();
	public static final String ACTION_LOCATION_UPDATE = "jp.aedmap.android.map.ACTION_LOCATION_UPDATE";

	Context ctx;
	OnLocationChangedListener mListener;

	public MyLocationSource(Context ctx) {
		this.ctx = ctx;
	}

	@Override
	public void activate(OnLocationChangedListener listener) {
		if (DEBUG) {
			Log.v(TAG, "activate");
		}
		mListener = listener;
		LocationManager lm = (LocationManager) ctx
				.getSystemService(Context.LOCATION_SERVICE);

		registerReceivers(lm);
		requestLocationUpdates(lm);
	}

	private void registerReceivers(LocationManager lm) {
		// GPSのステータス変更はListenerで登録します.
		lm.addGpsStatusListener(gpsStatusLitener);

		// WI-FIのステータス変更はintentでわかります.
		final IntentFilter wifiIntentFilter = new IntentFilter();
		wifiIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		wifiIntentFilter
				.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		wifiIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		ctx.registerReceiver(wifiStateUpdateReciever, wifiIntentFilter);

		// LocationProviderの変更
		final IntentFilter locationIntentFilter = new IntentFilter();
		locationIntentFilter
				.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
		ctx.registerReceiver(locationProviderReceiver, locationIntentFilter);

		if (DEBUG) {
			Log.v(TAG, "registerReceivers");
		}
	}

	private final BroadcastReceiver wifiStateUpdateReciever = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action == null) {
				return;
			}
			if (DEBUG) {
				Log.v(TAG, action);
			}
		}
	};

	private final BroadcastReceiver locationProviderReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action == null) {
				return;
			}
			if (DEBUG) {
				Log.v(TAG, action);
			}
		}

	};

	private Location prevLocation;

	private final LocationListener locationListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			if (DEBUG) {
				Log.v(TAG,
						"onLocationChanged:" + location.getProvider() + ":lat="
								+ location.getLatitude() + ",lng="
								+ location.getLongitude());
			}
			Intent intent = new Intent(ACTION_LOCATION_UPDATE);
			intent.putExtra(LocationManager.KEY_LOCATION_CHANGED, location);
			ctx.sendBroadcast(intent);
			if (mListener != null) {
				mListener.onLocationChanged(location);
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
			if (DEBUG) {
				Log.v(TAG, "onProviderDisabled:" + provider);
			}
		}

		@Override
		public void onProviderEnabled(String provider) {
			if (DEBUG) {
				Log.v(TAG, "onProviderEnabled:" + provider);
			}
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			if (DEBUG) {
				Log.v(TAG, "onStatusChanged:" + provider);
			}
		}
	};

	private final GpsStatus.Listener gpsStatusLitener = new GpsStatus.Listener() {
		/**
		 * このメソッドは、プロバイダの場所を取得することができない場合、 または最近使用不能の期間後に利用可能となっている場合に呼び出されます。
		 */
		@Override
		public void onGpsStatusChanged(int event) {
			// GpsStatus.Listenerで呼ばれる
			switch (event) {
			case GpsStatus.GPS_EVENT_STARTED: {
				Log.v(TAG, "GPS_EVENT_STARTED");
				break;
			}
			case GpsStatus.GPS_EVENT_STOPPED: {
				Log.v(TAG, "GPS_EVENT_STOPPED");
				break;
			}
			case GpsStatus.GPS_EVENT_FIRST_FIX: {
				Log.v(TAG, "GPS_EVENT_FIRST_FIX");
				break;
			}
			case GpsStatus.GPS_EVENT_SATELLITE_STATUS: {
				Log.v(TAG, "GPS_EVENT_SATELLITE_STATUS");
				break;
			}
			}
		}
	};

	private void requestLocationUpdates(final LocationManager lm) {

		for (String providerName : lm.getAllProviders()) {
			if (lm.isProviderEnabled(providerName)) {
				lm.requestLocationUpdates(providerName, 0, 0, locationListener);
				if (DEBUG) {
					Log.v(TAG, "requestLocationUpdates(" + providerName + ")");
				}
			} else {
				if (DEBUG) {
					Log.v(TAG, providerName + " is not active.");
				}
			}
		}
		final Criteria criteria = new Criteria();
		criteria.setBearingRequired(false); // 方位不要
		criteria.setSpeedRequired(false); // 速度不要
		criteria.setAltitudeRequired(false); // 高度不要
		final String provider = lm.getBestProvider(criteria, true);
		if (provider != null) {
			final Location lastKnownLocation = lm
					.getLastKnownLocation(provider);
			if (DEBUG) {
				Log.v(TAG,
						"lastKnownLocation:lat="
								+ lastKnownLocation.getLatitude() + ",lng="
								+ lastKnownLocation.getLongitude());
			}
			if (lastKnownLocation != null
					&& (new Date().getTime() - lastKnownLocation.getTime()) <= (5 * 60 * 1000L)) {
				Intent intent = new Intent(ACTION_LOCATION_UPDATE);
				intent.putExtra(LocationManager.KEY_LOCATION_CHANGED,
						lastKnownLocation);
				ctx.sendBroadcast(intent);
				if (mListener != null) {
					mListener.onLocationChanged(lastKnownLocation);
				}
			}
		}
	}

	@Override
	public void deactivate() {
		removeUpdates();
		if (DEBUG) {
			Log.v(TAG, "deactivate");
		}
		mListener = null;
	}

	private void removeUpdates() {
		LocationManager lm = (LocationManager) ctx
				.getSystemService(Context.LOCATION_SERVICE);
		lm.removeUpdates(locationListener);

		ctx.unregisterReceiver(wifiStateUpdateReciever);
		ctx.unregisterReceiver(locationProviderReceiver);
		if (DEBUG) {
			Log.v(TAG, "removeUpdates");
		}
	}
}
