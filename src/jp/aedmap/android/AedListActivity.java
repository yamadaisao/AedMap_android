package jp.aedmap.android;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.android.gms.maps.model.LatLng;

public class AedListActivity extends SherlockFragmentActivity {
	@SuppressWarnings("unused")
	private static final String TAG = AedListActivity.class.getSimpleName();
	@SuppressWarnings("unused")
	private static final boolean DEBUG = false;

	public static final String ARG_CURRENT = "current";

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.activity_aed_list);

		Bundle args = new Bundle();
		LatLng current = getIntent().getParcelableExtra(ARG_CURRENT);
		args.putParcelable(AedListFragment.ARG_CURRENT, current);
		Fragment fr = Fragment.instantiate(getApplicationContext(),
				AedListFragment.class.getName(), args);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.replace(R.id.fragment_container, fr);
		ft.commit();
	}
}
