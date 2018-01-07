package com.nkming.clipboard.model.room;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.text.TextUtils;

@Entity(tableName = ClipMeta.TABLE,
		indices = {@Index(value = {ClipMeta.COL_CREATE_AT}, unique = true)})
public class ClipMeta
{
	public static final String TABLE = "clip_metas";
	public static final String COL_ID = "id";
	public static final String COL_CREATE_AT = "create_at";
	public static final String COL_label = "label";

	public ClipMeta(long createAt, String label)
	{
		this.createAt = createAt;
		this.label = label;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof ClipMeta))
		{
			return false;
		}
		ClipMeta rhs = (ClipMeta)obj;
		return id == rhs.id && createAt == rhs.createAt
				&& TextUtils.equals(label, rhs.label);
	}

	@PrimaryKey(autoGenerate = true)
	public long id;

	@ColumnInfo(name = COL_CREATE_AT)
	public long createAt;

	public String label;
}
