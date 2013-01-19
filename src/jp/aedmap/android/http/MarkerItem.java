package jp.aedmap.android.http;

import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

/**
 * AED アイコンのデータです.
 * 
 * @author yamada.isao
 * 
 */
public class MarkerItem implements Parcelable {

	public static final int TYPE_ORIGNAL = 0;
	public static final int TYPE_NEW = 1;
	public static final int TYPE_EDIT = 2;
	public static final int TYPE_DELETE = 3;
	public static final int TYPE_HOT = 4;

	/** id */
	public Long id;

	public LatLng position;

	/** 名称 */
	public String name;
	/** 住所 */
	public String adr;
	/** 利用可能時間帯 */
	public String able;
	/** 情報源 */
	public String src;
	/** 特記事項 */
	public String spl;
	/** 更新日時. */
	public Date time;
	/** 中心からの距離(m) */
	public Long dist = 0L;

	public boolean isClickable = false;

	/** アイコンの種類 */
	public int type;

	/**
	 * コンストラクタ
	 * 
	 * @param id
	 *            ユニークなidです.
	 * @param point
	 *            AED設置情報
	 * @param title
	 *            設置場所
	 * @param snippet
	 *            説明
	 */
	public MarkerItem(long id, LatLng position, String name) {
		this.id = id;
		this.position = position;
		this.name = name;
	}

	private MarkerItem(Parcel in) {
		id = in.readLong();
		position = new LatLng(in.readDouble(), in.readDouble());
		name = in.readString();
		adr = in.readString();
		able = in.readString();
		src = in.readString();
		spl = in.readString();
		time = (Date) in.readSerializable();
		dist = in.readLong();
		type = in.readInt();
	}

	// override equals/hashCode
	@Override
	public boolean equals(Object obj) {
		// 引数が自分自身かどうか
		if (obj == this) {
			return true;
		}
		// 型のチェック、nullチェックも兼ねる
		if (!(obj instanceof MarkerItem)) {
			return false;
		}
		// このキャストの成功は保証されている
		MarkerItem item = (MarkerItem) obj;
		return id != null && id.equals(item.id);
	}

	@Override
	public int hashCode() {
		int hash = 1;
		hash = hash * 31 + id.hashCode();
		return hash;
	}

	// implements Parcelable
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeDouble(position.latitude);
		dest.writeDouble(position.longitude);
		dest.writeString(name);
		dest.writeString(adr);
		dest.writeLong(id);
		dest.writeString(able);
		dest.writeString(src);
		dest.writeString(spl);
		dest.writeSerializable(time);
		dest.writeLong(dist);
		dest.writeInt(type);
	}

	public static final Parcelable.Creator<MarkerItem> CREATOR = new Parcelable.Creator<MarkerItem>() {
		@Override
		public MarkerItem createFromParcel(Parcel in) {
			return new MarkerItem(in);
		}

		@Override
		public MarkerItem[] newArray(int size) {
			return new MarkerItem[size];
		}
	};
}
