package jp.aedmap.android;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.LocationSource;

/**
 * <ol>
 * <li>
 * First of all I check what providers are enabled. Some may be disabled on the
 * device, some may be disabled in application manifest.</li>
 * <li>
 * If any provider is available I start location listeners and timeout timer.
 * It's 20 seconds in my example, may not be enough for GPS so you can enlarge
 * it.</li>
 * <li>
 * If I get update from location listener I use the provided value. I stop
 * listeners and timer.</li>
 * <li>
 * If I don't get any updates and timer elapses I have to use last known values.
 * </li>
 * <li>
 * I grab last known values from available providers and choose the most recent
 * of them.</li>
 * </ol>
 * 
 * @author isao-pc2
 * 
 */
public class MyLocationSource implements LocationSource {

	private static final boolean DEBUG = true;
	private static final String TAG = MyLocationSource.class.getSimpleName();
	public static final String ACTION_LOCATION_UPDATE = "com.android.practice.map.ACTION_LOCATION_UPDATE";

	Timer timer1;
	LocationManager lm;
	boolean gpsEnabled = false;
	boolean networkEnabled = false;
	Context ctx;
	OnLocationChangedListener mListener;
	Location netLoc = null;
	Location gpsLoc = null;

	public MyLocationSource(Context ctx) {
		this.ctx = ctx;
	}

	@Override
	public void activate(OnLocationChangedListener listener) {
		if (DEBUG) {
			Log.v(TAG, "activate");
		}
		mListener = listener;
		netLoc = null;
		gpsLoc = null;
		getLocation();
	}

	@Override
	public void deactivate() {
		if (DEBUG) {
			Log.v(TAG, "deactivate");
		}
		cancelTimer();
	}

	private boolean getLocation() {
		// I use LocationResult callback class to pass location value from
		// MyLocation to user code.
		if (lm == null) {
			lm = (LocationManager) ctx
					.getSystemService(Context.LOCATION_SERVICE);
		}

		// exceptions will be thrown if provider is not permitted.
		try {
			gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch (Exception ex) {
		}
		try {
			networkEnabled = lm
					.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		} catch (Exception ex) {
		}

		// don't start listeners if no provider is enabled
		if (!gpsEnabled && !networkEnabled) {
			if (DEBUG) {
				Log.v(TAG, "gps and network isn't enabled");
			}
			return false;
		}

		if (gpsEnabled) {
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
					locationListenerGps);
		}
		if (networkEnabled) {
			lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0,
					locationListenerNetwork);
		}
		timer1 = new Timer();
		timer1.scheduleAtFixedRate(new GetLastLocation(), 0, ctx.getResources()
				.getInteger(R.integer.scheduleAtFixedRate));
		return true;
	}

	private void cancelTimer() {
		timer1.cancel();
		lm.removeUpdates(locationListenerGps);
		lm.removeUpdates(locationListenerNetwork);
	}

	LocationListener locationListenerGps = new LocationListener() {
		public void onLocationChanged(Location location) {
			broadcastMessage(location);
		}

		public void onProviderDisabled(String provider) {
			Log.v(TAG, provider + " disabled.");
			gpsEnabled = false;
			lm.removeUpdates(this);
			selectProvider();
		}

		public void onProviderEnabled(String provider) {
			Log.v(TAG, provider + " enabled.");
			gpsEnabled = true;
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
			selectProvider();
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			switch (status) {
			case LocationProvider.OUT_OF_SERVICE:
				Log.v(TAG, provider + " is out of service.");
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				Log.v(TAG, provider + " is temporarily unavailable.");
				break;
			case LocationProvider.AVAILABLE:
				Log.v(TAG, provider + " is available.");
				break;

			default:
				break;
			}
			selectProvider();
		}
	};

	LocationListener locationListenerNetwork = new LocationListener() {
		public void onLocationChanged(Location location) {
			broadcastMessage(location);
		}

		public void onProviderDisabled(String provider) {
			Log.v(TAG, provider + " disabled.");
			gpsEnabled = false;
			lm.removeUpdates(this);
			selectProvider();
		}

		public void onProviderEnabled(String provider) {
			Log.v(TAG, provider + " enabled.");
			gpsEnabled = true;
			lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0,
					this);
			selectProvider();
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			switch (status) {
			case LocationProvider.OUT_OF_SERVICE:
				Log.v(TAG, provider + " is out of service.");
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				Log.v(TAG, provider + " is temporarily unavailable.");
				break;
			case LocationProvider.AVAILABLE:
				Log.v(TAG, provider + " is available.");
				break;

			default:
				break;
			}
			selectProvider();
		}
	};

	class GetLastLocation extends TimerTask {
		@Override
		public void run() {
			if (gpsEnabled) {
				gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			}
			if (networkEnabled) {
				netLoc = lm
						.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			}

			// if there are both values use the latest one
			if (gpsLoc != null && netLoc != null) {
				if (gpsLoc.getTime() > netLoc.getTime()) {
					broadcastMessage(gpsLoc);
				} else {
					broadcastMessage(netLoc);
				}
				return;
			}

			if (gpsEnabled == true && gpsLoc != null) {
				broadcastMessage(gpsLoc);
				return;
			}
			if (netLoc != null) {
				broadcastMessage(netLoc);
				return;
			}
			broadcastMessage(null);
		}
	}

	private void broadcastMessage(Location loc) {
		if (loc != null) {
			timer1.cancel();
			Intent broadcastIntent = new Intent();
			broadcastIntent.putExtra(LocationManager.KEY_LOCATION_CHANGED, loc);
			broadcastIntent.setAction(ACTION_LOCATION_UPDATE);
			ctx.sendBroadcast(broadcastIntent);
			mListener.onLocationChanged(loc);
		}
	}

	private void selectProvider() {
		Location location = null;
		if (gpsEnabled) {
			location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		} else if (networkEnabled) {
			location = lm
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}
		if (location != null) {
			broadcastMessage(location);
		}
	}
}
