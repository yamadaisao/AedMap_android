package jp.aedmap.android;

import java.lang.ref.WeakReference;

import jp.aedmap.android.MyLocationSource.LocationResult;
import jp.aedmap.android.util.MapUtils;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;

public class AedMapActivity extends FragmentActivity {

	private static final String TAG = AedMapActivity.class.getSimpleName();

	private GoogleMap mMap;
	private MapHandler mapHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_aed_map);
		ctx = getApplicationContext();
		setUpMapIfNeeded();
	}

	@Override
	protected void onResume() {
		super.onResume();
		setUpMapIfNeeded();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_aed_map, menu);
		return true;
	}

	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the
		// map.
		if (mMap == null) {
			// Try to obtain the map from the SupportMapFragment.
			mMap = ((SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map)).getMap();
			// Check if we were successful in obtaining the map.
			if (mMap != null) {
				mapHandler = new MapHandler(mMap);
				setUpMap();
			}
		}
	}

	private static final int MSG_GOT_LOCATION = 1;
	private static final String MSG_DATA_LOCATION = "Location";
	private static Context ctx;

	static class MapHandler extends Handler {
		private final WeakReference<GoogleMap> _map;

		public MapHandler(GoogleMap map) {
			_map = new WeakReference<GoogleMap>(map);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case MSG_GOT_LOCATION: {
				Bundle data = msg.getData();
				Location location = (Location) data.get(MSG_DATA_LOCATION);
				GoogleMap map = _map.get();

				// float accuracy = MapUtils.calcZoom(location.getAccuracy());
				float zoomLevel = MapUtils.calculateZoomLevel(ctx,
						location.getAccuracy());
				// zoomLevel -= 3;
				Log.v(TAG, "zoom level=" + zoomLevel);
				map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
						location.getLatitude(), location.getLongitude()),
						zoomLevel));
				break;
			}

			default:
				break;
			}
		}
	}

	private void setUpMap() {
		mMap.setMyLocationEnabled(true);
		UiSettings uiSetting = mMap.getUiSettings();
		uiSetting.setAllGesturesEnabled(true);
		uiSetting.setCompassEnabled(true);
		uiSetting.setMyLocationButtonEnabled(true);

		LocationResult locationResult = new LocationResult() {
			@Override
			public void gotLocation(Location location) {
				Message msg = new Message();
				msg.what = MSG_GOT_LOCATION;
				Bundle data = new Bundle();
				data.putParcelable(MSG_DATA_LOCATION, location);
				msg.setData(data);
				mapHandler.sendMessage(msg);
			}
		};
		mMap.setLocationSource(new MyLocationSource(this, locationResult));
	}
}
