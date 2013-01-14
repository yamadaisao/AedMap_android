package jp.aedmap.android.http;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import jp.aedmap.android.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

import com.google.android.gms.maps.model.LatLng;

public class MarkerQueryAsyncTask extends
		AsyncTask<LatLng, Integer, AsyncTaskResult<MarkerItemResult>> {

	private static final boolean DEBUG = true;
	private static final String TAG = MarkerQueryAsyncTask.class
			.getSimpleName();
	private static final String QUERY_URL = "http://aedm.jp/toxmltest.php";

	private final AsyncTaskCallback<MarkerItemResult> callback;

	public MarkerQueryAsyncTask(AsyncTaskCallback<MarkerItemResult> callback) {
		this.callback = callback;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	@Override
	protected AsyncTaskResult<MarkerItemResult> doInBackground(LatLng... param) {
		MarkerItemResult result = null;

		String strUrl = String.format("%s?lat=%s&lng=%s", QUERY_URL,
				param[0].latitude, param[0].longitude);
		SimpleDateFormat fomatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss",
				Locale.JAPAN);
		Calendar now = Calendar.getInstance();
		now.add(Calendar.MONTH, -1);
		Date nowDate = now.getTime();
		try {
			HttpURLConnection con = null;
			// URLの作成
			URL url = new URL(strUrl);
			// 接続用HttpURLConnectionオブジェクト作成
			con = (HttpURLConnection) url.openConnection();
			// リクエストメソッドの設定
			con.setRequestMethod("GET");
			// リダイレクトを自動で許可しない設定
			con.setInstanceFollowRedirects(false);
			// ヘッダーの設定(複数設定可能)
			Locale locale = Locale.getDefault();
			con.setRequestProperty("accept-Language", locale.getLanguage());

			// 接続
			con.connect();

			BufferedInputStream is = new BufferedInputStream(
					con.getInputStream());
			result = new MarkerItemResult(param[0]);
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(is, "UTF-8");
			// ・XmlPullParser.START_DOCUMENT
			// ・XmlPullParser.START_TAG
			// ・XmlPullParser.TEXT
			// ・XmlPullParser.END_TAG
			// ・XmlPullParser.END_DOCUMENT
			for (int e = parser.getEventType(); e != XmlPullParser.END_DOCUMENT; e = parser
					.next()) {
				switch (e) {
				case XmlPullParser.START_TAG:
					try {
						// other tag : markers
						if ("marker".equals(parser.getName())) {
							long id = Long.parseLong(parser.getAttributeValue(
									null, "id"));
							double lat = Double.parseDouble(parser
									.getAttributeValue(null, "lat"));
							double lng = Double.parseDouble(parser
									.getAttributeValue(null, "lng"));

							// 日本限定なので、西経・南緯は気にしない.
							result.minLatitude = Math.min(result.minLatitude,
									lat);
							result.minLongitude = Math.min(result.minLongitude,
									lng);
							result.maxLatitude = Math.max(result.maxLatitude,
									lat);
							result.maxLongitude = Math.max(result.maxLongitude,
									lng);
							String name = parser
									.getAttributeValue(null, "name");

							MarkerItem marker = new MarkerItem(id, new LatLng(
									lat, lng), name);
							marker.adr = parser.getAttributeValue(null, "adr");
							marker.able = parser
									.getAttributeValue(null, "able");
							marker.src = parser.getAttributeValue(null, "src");
							marker.spl = parser.getAttributeValue(null, "spl");
							String time = parser
									.getAttributeValue(null, "time");
							try {
								marker.time = fomatter.parse(time);
								if (nowDate.before(marker.time)) {
									marker.type = MarkerItem.TYPE_HOT;
								}
							} catch (ParseException e1) {
								Log.e(TAG, "time=" + time);
								e1.printStackTrace();
								marker.time = new Date();
							}
							result.markers.add(marker);
						}
					} catch (NumberFormatException ex) {
						Log.e(TAG, "NumberFormatException");
						ex.printStackTrace();
					}
					break;
				case XmlPullParser.END_TAG:
					break;
				case XmlPullParser.START_DOCUMENT:
				case XmlPullParser.END_DOCUMENT:
				case XmlPullParser.TEXT:
				default:
					break;
				}
			}

			is.close();
			// 少しだけ範囲を狭くする
			result.minLatitude += (param[0].latitude - result.minLatitude) / 3;
			result.minLongitude += (param[0].longitude - result.minLongitude) / 3;
			result.maxLatitude -= (result.maxLatitude - param[0].latitude) / 3;
			result.maxLongitude -= (result.maxLongitude - param[0].longitude) / 3;
			if (DEBUG) {
				Log.v(TAG, String.format("q=(%f,%f), bounds(%f,%f)-(%f,%f)",
						result.queryLatitude, result.queryLongitude,
						result.minLatitude, result.minLongitude,
						result.maxLatitude, result.maxLongitude));
			}
			return AsyncTaskResult.createNormalResult(result);

		} catch (IllegalStateException e) {
			Log.e(TAG, "illegal state");
			e.printStackTrace();
			return AsyncTaskResult
					.createErrorResult(R.string.http_illegal_state);
		} catch (UnknownHostException e) {
			Log.e(TAG, "unknown host:" + e.getMessage());
			e.printStackTrace();
			return AsyncTaskResult
					.createErrorResult(R.string.http_unknown_host);
		} catch (IOException e) {
			Log.e(TAG, "io error");
			e.printStackTrace();
			return AsyncTaskResult.createErrorResult(R.string.http_io_error);
		} catch (XmlPullParserException e) {
			Log.e(TAG, "XmlPullParserException");
			e.printStackTrace();
			return AsyncTaskResult.createErrorResult(R.string.http_parse_error);
		}
	}

	@Override
	protected void onPostExecute(AsyncTaskResult<MarkerItemResult> result) {
		if (result.isError()) {
			if (result.getResId() == 0) {
				callback.onAppFailed(result.getResult());
			} else {
				callback.onFailed(result.getResId(), (String[]) null);
			}
		} else {
			callback.onSuccess(result.getResult());
		}
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
	}
}
