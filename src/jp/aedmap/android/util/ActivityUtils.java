package jp.aedmap.android.util;

import jp.aedmap.android.HelpActivity;
import jp.aedmap.android.R;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

public class ActivityUtils {
	/**
	 * Start activity help view.
	 * 
	 * @param ctx
	 * @param url
	 */
	public static void openHelp(Activity ctx, String url) {
		Intent intent = new Intent(ctx, HelpActivity.class);
		intent.putExtra(HelpActivity.ARG_URL, url);
		ctx.startActivity(intent);
	}

	/**
	 * Start activity dial to ambulance.
	 * 
	 * @param ctx
	 */
	public static void dialAmbulance(Activity ctx) {
		Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(ctx
				.getString(R.string.url_ambulance)));
		ctx.startActivity(intent);
	}
}
