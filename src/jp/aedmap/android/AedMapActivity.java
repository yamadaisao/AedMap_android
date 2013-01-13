package jp.aedmap.android;

import java.io.IOException;
import java.lang.ref.WeakReference;

import jp.aedmap.android.http.AsyncTaskCallback;
import jp.aedmap.android.http.MarkerItem;
import jp.aedmap.android.http.MarkerItemResult;
import jp.aedmap.android.http.MarkerQueryAsyncTask;
import jp.aedmap.android.util.GeocodeManager;
import jp.aedmap.android.util.MapUtils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class AedMapActivity extends FragmentActivity {

	private static final String TAG = AedMapActivity.class.getSimpleName();

	private static AddressHandler addrhandler;
	private GoogleMap mMap;
	private ProgressBar progress;

	private Context ctx;
	private boolean isFirst = true;

	BroadcastReceiver locationUpdateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action == null) {
				return;
			}

			// Location情報を取得
			Location loc = (Location) intent.getExtras().get(
					LocationManager.KEY_LOCATION_CHANGED);
			LatLng latlng = new LatLng(loc.getLatitude(), loc.getLongitude());
			getMarkers(latlng);

			if (MyLocationSource.ACTION_LOCATION_UPDATE.equals(action)) {
				if (isFirst) {
					moveCamera(loc);
					isFirst = false;
				}
			}
		}

	};

	private void moveCamera(Location loc) {
		LatLng latlng = new LatLng(loc.getLatitude(), loc.getLongitude());
		// Cameraの移動
		float zoomLevel = MapUtils.calculateZoomLevel(ctx, loc.getAccuracy());
		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoomLevel));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_aed_map);
		ctx = getApplicationContext();

		// プログレスの設定
		progress = (ProgressBar) findViewById(R.id.progress);
		progress.setVisibility(View.INVISIBLE);
		progress.setIndeterminate(true);

		TextView address = (TextView) findViewById(R.id.text_address);
		addrhandler = new AddressHandler(address);
	}

	@Override
	protected void onResume() {
		super.onResume();

		// レシーバの登録
		final IntentFilter locationIntentFilter = new IntentFilter();
		locationIntentFilter.addAction(MyLocationSource.ACTION_LOCATION_UPDATE);
		registerReceiver(locationUpdateReceiver, locationIntentFilter);

		setUpMapIfNeeded();
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(locationUpdateReceiver);
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
		mMap.setMyLocationEnabled(true);
		UiSettings uiSetting = mMap.getUiSettings();
		uiSetting.setAllGesturesEnabled(true);
		uiSetting.setCompassEnabled(true);
		uiSetting.setMyLocationButtonEnabled(true);
		mMap.setOnCameraChangeListener(new OnCameraChangeListener() {

			@Override
			public void onCameraChange(CameraPosition position) {
				SharedData data = SharedData.getInstance();
				if (data.getLastResult() == null) {
					data.setLastResult(new MarkerItemResult(position.target));
				}

				// 取得済みの矩形範囲を超えた場合に再取得する
				MarkerItemResult lastResult = data.getLastResult();
				if (position.target.latitude < lastResult.minLatitude
						|| position.target.latitude > lastResult.maxLatitude
						|| position.target.longitude < lastResult.minLongitude
						|| position.target.longitude > lastResult.maxLongitude) {
					getMarkers(position.target);
				}
				getAddress(position.target);
			}
		});
		mMap.setLocationSource(new MyLocationSource(ctx));
	}

	/**
	 * 現在地の緯度経度をパラメータにマーカーを取得します.
	 * 
	 * @param geoPoint
	 *            現在地の緯度経度
	 */
	private void getMarkers(LatLng position) {
		AsyncTaskCallback<MarkerItemResult> callback = new AsyncTaskCallback<MarkerItemResult>() {

			Context ctx = getApplicationContext();

			@Override
			public void onSuccess(MarkerItemResult result) {
				SharedData data = SharedData.getInstance();
				data.setLastResult(result);
				for (MarkerItem item : result.markers) {
					Marker marker = mMap
							.addMarker(MapUtils.createOptions(item));
				}
				progress.setVisibility(View.INVISIBLE);
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
		task.execute(position);
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
				}

				// 住所をメッセージに持たせて
				// ハンドラにUIを書き換えさせる
				Message message = new Message();
				Bundle bundle = new Bundle();
				bundle.putString(CURRENT_ADDRESS, strAddress);
				message.setData(bundle);
				addrhandler.sendMessage(message);
			}
		};
		searchAdress.start();
	}

	static class AddressHandler extends Handler {

		private WeakReference<TextView> address_;

		public AddressHandler(TextView address) {
			address_ = new WeakReference<TextView>(address);
		}

		@Override
		public void handleMessage(Message msg) {
			String str_address = msg.getData().get(CURRENT_ADDRESS).toString();
			TextView address = address_.get();
			address.setText(str_address);
		}
	}
}
