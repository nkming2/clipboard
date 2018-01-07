package com.nkming.clipboard.model.room;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity(tableName = ClipMime.TABLE,
		foreignKeys = @ForeignKey(entity = ClipMeta.class,
				parentColumns = ClipMeta.COL_ID,
				childColumns = ClipMime.COL_CLIP_ID, onDelete = CASCADE))
public class ClipMime
{
	public static final String TABLE = "clip_mimes";
	public static final String COL_ID = "id";
	public static final String COL_CLIP_ID = "clip_id";
	public static final String COL_MIME = "mime";

	public ClipMime(@NonNull String mime)
	{
		this.mime = mime;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof ClipMime))
		{
			return false;
		}
		ClipMime rhs = (ClipMime)obj;
		return id == rhs.id && clipId == rhs.clipId
				&& TextUtils.equals(mime, rhs.mime);
	}

	@PrimaryKey(autoGenerate = true)
	public long id;

	@ColumnInfo(name = COL_CLIP_ID, index = true)
	public long clipId;

	@NonNull
	public String mime;
}
