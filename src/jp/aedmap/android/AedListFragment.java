package jp.aedmap.android;

import java.util.Collections;
import java.util.List;

import jp.aedmap.android.http.MarkerItem;
import jp.aedmap.android.util.MapUtils;
import jp.aedmap.android.util.MapUtils.MarkerComparator;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

public class AedListFragment extends Fragment {

	public static final String ARG_CURRENT = "current";

	View view;
	ListView listView;
	View emptyView;
	Resources res;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		view = inflater.inflate(R.layout.fragment_aed_list, null);
		listView = (ListView) view.findViewById(R.id.aed_list_view);
		emptyView = view.findViewById(R.id.empty_view);
		listView.setEmptyView(emptyView);

		res = getActivity().getResources();
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		SharedData data = SharedData.getInstance();
		LatLng current = getArguments().getParcelable(ARG_CURRENT);
		if (data.getLastResult() != null) {
			List<MarkerItem> list = data.getLastResult().markers;
			for (MarkerItem item : list) {
				item.dist = MapUtils.getDistance(current, item.position);
			}
			Collections.sort(list, new MarkerComparator());
			// TextView の autoLinkがある場合は、
			// getApplicationContextではなくthisを渡さないといけない.
			listView.setAdapter(new AedListAdapter(getActivity(), data
					.getLastResult().markers));
		}
	}

	/**
	 * ViewHolder クラス. ListViewの各行です.
	 * 
	 * @author yamada.isao
	 * 
	 */
	static class ViewHolder {
		/** 設置場所 */
		TextView name;
		/** 住所 */
		TextView adr;
		/** 利用可能時間帯 */
		TextView able;
		/** 情報源 */
		TextView src;
		/** 補足 */
		TextView spl;
		/** 距離 */
		TextView dist;
	}

	/**
	 * カスタムリスト用のadapter.
	 * 
	 * @author yamada.isao
	 * 
	 */
	class AedListAdapter extends ArrayAdapter<MarkerItem> {

		private final LayoutInflater inflater;

		public AedListAdapter(Context context, List<MarkerItem> objects) {
			super(context, 0, objects);
			this.inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// Viewの取得
			ViewHolder holder;
			View view = convertView;
			if (view == null) {
				view = inflater.inflate(R.layout.aed_list_row, null);
				holder = new ViewHolder();
				holder.name = (TextView) view.findViewById(R.id.row_name);
				holder.adr = (TextView) view.findViewById(R.id.row_adr);
				holder.able = (TextView) view.findViewById(R.id.row_able);
				holder.src = (TextView) view.findViewById(R.id.row_src);
				holder.spl = (TextView) view.findViewById(R.id.row_spl);
				holder.dist = (TextView) view.findViewById(R.id.row_dist);
				view.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			// データの設定
			MarkerItem item = getItem(position);
			if (item != null) {
				holder.name.setText(item.name);
				holder.adr.setText(item.adr);
				holder.able.setText(item.able);
				holder.src.setText(res.getString(R.string.label_src_info,
						item.src));
				holder.spl.setText(res.getString(R.string.label_spl_info,
						item.spl));
				holder.dist.setText(res.getString(R.string.label_dist,
						item.dist));
			}
			return view;
		}
	}
}
