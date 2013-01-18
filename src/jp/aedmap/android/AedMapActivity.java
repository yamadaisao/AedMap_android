package jp.aedmap.android;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jp.aedmap.android.http.AsyncTaskCallback;
import jp.aedmap.android.http.MarkerItem;
import jp.aedmap.android.http.MarkerItemResult;
import jp.aedmap.android.http.MarkerQueryAsyncTask;
import jp.aedmap.android.util.GeocodeManager;
import jp.aedmap.android.util.MapUtils;
import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;

/**
 * Main activity of the aed map.
 * 
 * @author yamada.isao@gmail.com
 * 
 */
public class AedMapActivity extends FragmentActivity implements LocationSource,
		LocationListener {

	private static final String TAG = AedMapActivity.class.getSimpleName();
	private static final boolean DEBUG = true;
	private static final boolean DEBUG_LIFE_CYCLE = false;
	private static final boolean DEBUG_EVENT = true;

	private static MapHandler mapHandler;
	private GoogleMap mMap;
	private ProgressBar progress;
	private LocationManager lm = null;

	private Context ctx;
	private boolean isFirst = true;
	private OnLocationChangedListener mListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_aed_map);
		ctx = getApplicationContext();

		// プログレスの設定
		progress = (ProgressBar) findViewById(R.id.progress);
		progress.setVisibility(View.INVISIBLE);
		progress.setIndeterminate(true);

		lm = (LocationManager) getSystemService(LOCATION_SERVICE);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (DEBUG_LIFE_CYCLE) {
			Log.v(TAG, "onResume");
		}

		setUpMapIfNeeded();
		for (String providerName : lm.getAllProviders()) {
			if (lm.isProviderEnabled(providerName)) {
				lm.requestLocationUpdates(providerName, 0, 0, this);
				if (DEBUG) {
					Log.v(TAG, "requestLocationUpdates(" + providerName + ")");
				}
			} else {
				if (DEBUG) {
					Log.v(TAG, providerName + " is not active.");
				}
			}
		}

		TextView address = (TextView) findViewById(R.id.text_address);
		mapHandler = new MapHandler(mMap, address);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (DEBUG_LIFE_CYCLE) {
			Log.v(TAG, "onPause");
		}
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
				setUpMap();
			}
		}
	}

	private void setUpMap() {
		UiSettings uiSetting = mMap.getUiSettings();
		uiSetting.setAllGesturesEnabled(true);
		uiSetting.setCompassEnabled(true);
		uiSetting.setMyLocationButtonEnabled(true);
		mMap.setMyLocationEnabled(true);

		mMap.setOnCameraChangeListener(new OnCameraChangeListener() {

			@Override
			public void onCameraChange(CameraPosition position) {
				SharedData data = SharedData.getInstance();
				getMarkers(position.target.latitude, position.target.longitude);
				MarkerItemResult lastResult = data.getLastResult();
				if (position != null
						&& (lastResult == null
								|| position.target.latitude != lastResult.queryLatitude || position.target.longitude != lastResult.queryLongitude)) {
					getAddress(position.target);
				}
			}
		});
		mMap.setLocationSource(this);
		mMap.setInfoWindowAdapter(new CustomInfoAdapter());
	}

	@Override
	public void onLocationChanged(Location location) {
		if (DEBUG_EVENT) {
			Log.v(TAG,
					"onLocationChanged:" + location.getProvider() + ":lat="
							+ location.getLatitude() + ",lng="
							+ location.getLongitude());
		}
		if (mListener != null) {
			mListener.onLocationChanged(location);
			if (isFirst) {
				moveCamera(location);
				isFirst = false;
			}
		}
		getMarkers(location.getLatitude(), location.getLongitude());
	}

	private static Map<Long, Marker> markerMap = new HashMap<Long, Marker>();
	private static Map<Marker, MarkerItem> itemMap = new HashMap<Marker, MarkerItem>();

	/**
	 * パラメータの緯度経度をクエリにマーカーを取得します. <br/>
	 * 移動がの許容範囲以内の場合はマーカーを取得しません.
	 * 
	 * @param latitude
	 *            クエリの緯度
	 * @param longitude
	 *            クエリの経度
	 */
	private void getMarkers(double latitude, double longitude) {
		SharedData data = SharedData.getInstance();
		MarkerItemResult lastResult = data.getLastResult();
		if (lastResult == null || latitude < lastResult.minLatitude
				|| latitude > lastResult.maxLatitude
				|| longitude < lastResult.minLongitude
				|| longitude > lastResult.maxLongitude) {

			AsyncTaskCallback<MarkerItemResult> callback = new AsyncTaskCallback<MarkerItemResult>() {

				Context ctx = getApplicationContext();

				@Override
				public void onSuccess(MarkerItemResult result) {
					SharedData data = SharedData.getInstance();
					data.setLastResult(result);
					synchronized (markerMap) {
						for (MarkerItem item : result.markers) {
							if (markerMap.containsKey(item.id)) {
								// 取得済みのマーカーは置き換える
								// InfoWindowを表示している場合は置き換えない
								Marker oldMarker = markerMap.get(item.id);
								if (oldMarker.isInfoWindowShown() == false) {
									oldMarker.remove();
									Marker marker = mMap.addMarker(MapUtils
											.createOptions(item));
									markerMap.put(item.id, marker);
									itemMap.put(marker, item);
								}
							} else {
								Marker marker = mMap.addMarker(MapUtils
										.createOptions(item));
								markerMap.put(item.id, marker);
								itemMap.put(marker, item);
							}
						}
					}
					progress.setVisibility(View.INVISIBLE);

					if (DEBUG) {
						Log.v(TAG, "result=" + result.markers.size() + ",map="
								+ markerMap.size());
					}
					if (markerMap.size() >= 200) {
						Message msg = new Message();
						msg.what = MSG_REMOVE;
						mapHandler.sendMessage(msg);
					}
				}

				@Override
				public void onFailed(int resId, String... args) {
					Toast.makeText(ctx, resId, Toast.LENGTH_SHORT).show();
					progress.setVisibility(View.INVISIBLE);
				}

				@Override
				public void onAppFailed(MarkerItemResult data) {
					progress.setVisibility(View.INVISIBLE);
				}

			};
			progress.setVisibility(View.VISIBLE);
			MarkerQueryAsyncTask task = new MarkerQueryAsyncTask(callback);
			task.execute(new LatLng(latitude, longitude));
		}

	}

	private void moveCamera(Location loc) {
		LatLng latlng = new LatLng(loc.getLatitude(), loc.getLongitude());
		// Cameraの移動
		float zoomLevel = MapUtils.calculateZoomLevel(ctx, loc.getAccuracy());
		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoomLevel));
	}

	private static final String CURRENT_ADDRESS = "str_address";

	/**
	 * 住所を取得します.<br/>
	 * パフォーマンスが悪かったのでスレッドで実行します.
	 * 
	 * @param geoPoint
	 *            緯度経度
	 */
	private void getAddress(final LatLng latlng) {
		Thread searchAdress = new Thread() {
			@Override
			public void run() {
				// 場所名を文字列で取得する
				String strAddress = null;
				try {
					// 住所を取得
					Address address = GeocodeManager.point2address(
							latlng.latitude, latlng.longitude, ctx);
					strAddress = GeocodeManager.concatAddress(address);
				} catch (IOException e) {
					strAddress = getString(R.string.msg_location_fail);
					Log.e(TAG, e.getMessage());
				}

				// 住所をメッセージに持たせて
				// ハンドラにUIを書き換えさせる
				Message message = new Message();
				message.what = MSG_ADDRESS;
				Bundle bundle = new Bundle();
				bundle.putString(CURRENT_ADDRESS, strAddress);
				message.setData(bundle);
				mapHandler.sendMessage(message);
			}
		};
		searchAdress.start();
	}

	private static final int MSG_ADDRESS = 1;
	private static final int MSG_REMOVE = 2;

	static class MapHandler extends Handler {

		private final WeakReference<TextView> address_;
		private final WeakReference<GoogleMap> map_;

		public MapHandler(GoogleMap map, TextView address) {
			address_ = new WeakReference<TextView>(address);
			map_ = new WeakReference<GoogleMap>(map);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_ADDRESS: {
				String str_address = msg.getData().get(CURRENT_ADDRESS)
						.toString();
				TextView address = address_.get();
				address.setText(str_address);
				break;
			}
			case MSG_REMOVE: {
				GoogleMap map = map_.get();
				LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
				// Loop through all the items that are available to be placed on
				// the map
				synchronized (markerMap) {
					int initSize = markerMap.size();
					for (Iterator<Long> i = markerMap.keySet().iterator(); i
							.hasNext();) {
						Marker marker = markerMap.get(i.next());
						// If the item is within the the bounds of the screen
						if (bounds.contains(marker.getPosition()) == false) {
							if (marker.isInfoWindowShown() == false) {
								itemMap.remove(marker);
								i.remove();
							}
						}
					}
					if (DEBUG) {
						Log.v(TAG, "markerMap reduced " + initSize + " to "
								+ markerMap.size() + "," + itemMap.size());
					}
				}
				break;
			}
			default:
				break;
			}
		}
	}

	/**
	 * InfoWindow
	 * 
	 * @author yamada.isao@gmail.com
	 * 
	 */
	private class CustomInfoAdapter implements InfoWindowAdapter {
		/** Window の View. */
		private final View mWindow;
		private final TextView name;
		private final TextView adr;
		private final TextView able;
		private final TextView src;
		private final TextView spl;

		public CustomInfoAdapter() {
			mWindow = getLayoutInflater().inflate(R.layout.aed_info_window,
					null);

			name = (TextView) mWindow.findViewById(R.id.name);
			adr = (TextView) mWindow.findViewById(R.id.adr);
			able = (TextView) mWindow.findViewById(R.id.able);
			src = (TextView) mWindow.findViewById(R.id.src);
			spl = (TextView) mWindow.findViewById(R.id.spl);
		}

		@Override
		public View getInfoContents(Marker marker) {
			return null;
		}

		@Override
		public View getInfoWindow(Marker marker) {
			MarkerItem item = itemMap.get(marker);
			name.setText(item.name);
			if (item.adr != null && "".equals(item.adr.trim()) == false) {
				adr.setText(item.adr);
				adr.setVisibility(View.VISIBLE);
			} else {
				adr.setVisibility(View.GONE);
			}
			if (item.able != null && "".equals(item.able.trim()) == false) {
				able.setText(item.able);
				able.setVisibility(View.VISIBLE);
			} else {
				able.setVisibility(View.GONE);
			}
			if (item.src != null && "".equals(item.src.trim()) == false) {
				src.setText(item.src);
				src.setVisibility(View.VISIBLE);
			} else {
				src.setVisibility(View.GONE);
			}
			if (item.spl != null && "".equals(item.spl.trim()) == false) {
				spl.setText(item.spl);
				spl.setVisibility(View.VISIBLE);
			} else {
				spl.setVisibility(View.GONE);
			}
			return mWindow;
		}
	}

	// --------------------------------------------------------------------------------
	// unused methods
	// --------------------------------------------------------------------------------
	@Override
	public void activate(OnLocationChangedListener listener) {
		mListener = listener;
		if (DEBUG_LIFE_CYCLE) {
			Log.d(TAG, "activate");
		}
	}

	@Override
	public void deactivate() {
		mListener = null;
		if (DEBUG_LIFE_CYCLE) {
			Log.d(TAG, "deactivate");
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		if (DEBUG_EVENT) {
			Log.v(TAG, "onProviderDisabled:" + provider);
		}
	}

	@Override
	public void onProviderEnabled(String provider) {
		if (DEBUG_EVENT) {
			Log.v(TAG, "onProviderEnabled:" + provider);
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		if (DEBUG_EVENT) {
			Log.v(TAG, "onStatusChanged:" + provider);
		}
	}
}
