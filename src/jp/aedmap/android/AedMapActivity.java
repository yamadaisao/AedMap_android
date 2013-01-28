package jp.aedmap.android;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jp.aedmap.android.http.AsyncTaskCallback;
import jp.aedmap.android.http.MarkerItem;
import jp.aedmap.android.http.MarkerItemResult;
import jp.aedmap.android.http.MarkerQueryAsyncTask;
import jp.aedmap.android.util.ActivityUtils;
import jp.aedmap.android.util.GeocodeManager;
import jp.aedmap.android.util.MapUtils;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.maps.GeoPoint;

/**
 * Main activity of the aed map.
 * 
 * @author yamada.isao@gmail.com
 * 
 */
public class AedMapActivity extends SherlockFragmentActivity implements
		LocationSource, LocationListener {

	private static final String TAG = AedMapActivity.class.getSimpleName();
	private static final boolean DEBUG = true;
	private static final boolean DEBUG_LIFE_CYCLE = false;
	private static final boolean DEBUG_EVENT = true;

	private static MapHandler mapHandler;
	private GoogleMap mMap;
	private ProgressBar progress;
	private View icList;
	private LocationManager lm = null;

	private Marker infoMarker = null;
	private View btnAdd;
	private View btnEdit;

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

		// リストアイコン
		icList = findViewById(R.id.ic_list);
		icList.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(ctx, AedListActivity.class);
				intent.putExtra(AedListActivity.ARG_CURRENT,
						mMap.getCameraPosition().target);
				startActivity(intent);
			}
		});
		lm = (LocationManager) getSystemService(LOCATION_SERVICE);

		// ---------------------------------------------------------
		// ボタンバー
		// ---------------------------------------------------------
		View resuscitation = findViewById(R.id.btn_resuscitation);
		resuscitation.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				ActivityUtils.openHelp(AedMapActivity.this,
						getString(R.string.url_resuscitation));
			}
		});
		View ambulance = findViewById(R.id.btn_ambulance);
		ambulance.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				ActivityUtils.dialAmbulance(AedMapActivity.this);
			}
		});

		btnAdd = findViewById(R.id.btn_add);
		btnEdit = findViewById(R.id.btn_edit);
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
				checkInfoWindow();
			}
		});

		mMap.setOnMapClickListener(new OnMapClickListener() {

			@Override
			public void onMapClick(LatLng point) {
				checkInfoWindow();
			}
		});
		mMap.setLocationSource(this);
		mMap.setInfoWindowAdapter(new CustomInfoAdapter());
		mMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {

			@Override
			public void onInfoWindowClick(Marker marker) {
				Toast.makeText(ctx, "InfoWindow.clicked", Toast.LENGTH_SHORT)
						.show();
			}
		});

	}

	private void checkInfoWindow() {
		if (infoMarker != null) {
			if (infoMarker.isInfoWindowShown() == false) {
				btnAdd.setVisibility(View.VISIBLE);
				btnEdit.setVisibility(View.GONE);
				infoMarker = null;
			}
		}
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

	private void moveCamera(Address addr) {
		LatLng latlng = new LatLng(addr.getLatitude(), addr.getLongitude());
		// Cameraの移動
		float zoomLevel = 16f;
		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoomLevel));
	}

	private void moveCamera(AddressView addr) {
		LatLng latlng = new LatLng(addr.latitude, addr.longitude);
		// Cameraの移動
		float zoomLevel = 16f;
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.activity_aed_map, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_search: {
			onSearchRequested();
			break;
		}

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private AlertDialog dialog = null;

	/**
	 * 検索ダイアログで入力された文字列で住所検索を行います.
	 */
	@Override
	public void onNewIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);

			// GeoCoderで地名検索させて、Addressに変換させた。
			List<Address> addressList = GeocodeManager.address2Point(query,
					this);
			if (addressList == null) {
				Toast.makeText(this, R.string.msg_query_fail, Toast.LENGTH_LONG)
						.show();
			} else {
				if (addressList.size() == 0) {
					Toast.makeText(this, R.string.msg_result_zero,
							Toast.LENGTH_LONG).show();
				} else if (addressList.size() == 1) {
					Address address = addressList.get(0);
					moveCamera(address);
				} else {
					final List<AddressView> rows = new ArrayList<AddressView>();
					for (Address address : addressList) {
						AddressView row = new AddressView();
						row.address = GeocodeManager.concatAddress(address);
						row.latitude = address.getLatitude();
						row.longitude = address.getLongitude();
						rows.add(row);
					}
					ListView lv = new ListView(this);
					lv.setAdapter(new AddressAdapter(this,
							R.layout.address_list, rows));
					lv.setScrollingCacheEnabled(false);
					lv.setOnItemClickListener(new OnItemClickListener() {
						@Override
						public void onItemClick(AdapterView<?> items,
								View view, int position, long id) {
							dialog.dismiss();
							moveCamera(rows.get(position));
						}
					});
					dialog = new AlertDialog.Builder(this)
							.setTitle(R.string.msg_some_location)
							.setPositiveButton(R.string.button_cancel, null)
							.setView(lv).create();
					dialog.show();

				}
			}
		}
	}

	/**
	 * 住所候補のViewHolder
	 * 
	 * @author yamadaisao
	 * 
	 */
	static class ViewHolder {
		TextView address;
	}

	/**
	 * 住所候補のデータ
	 * 
	 * @author yamadaisao
	 * 
	 */
	class AddressView {
		String address;
		double latitude;
		double longitude;
	}

	/**
	 * 住所検索の結果が複数あった場合のリスト表示用Adapter
	 * 
	 * @author yamadaisao
	 * 
	 */
	private class AddressAdapter extends ArrayAdapter<AddressView> {
		private final LayoutInflater inflater;
		private final List<AddressView> list;
		private final int rowLayout;

		public AddressAdapter(Context context, int textViewResourceId,
				List<AddressView> objects) {
			super(context, textViewResourceId, objects);
			inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			list = objects;
			rowLayout = textViewResourceId;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			ViewHolder holder;
			if (convertView == null) {
				holder = new ViewHolder();
				view = inflater.inflate(rowLayout, null);
				holder.address = (TextView) view.findViewById(R.id.address);
				view.setTag(holder);
			} else {
				holder = (ViewHolder) view.getTag();
			}
			AddressView address = list.get(position);
			if (address != null) {
				holder.address.setText(address.address);
			}
			return view;
		}
	}

	public void moveToSearchResult(GeoPoint geoPoint) {
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
	 * マーカーをタップした時に表示をするWindowです.
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

			btnAdd.setVisibility(View.GONE);
			btnEdit.setVisibility(View.VISIBLE);
			infoMarker = marker;
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
