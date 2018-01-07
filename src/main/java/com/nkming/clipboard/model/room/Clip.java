package com.nkming.clipboard.model.room;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Relation;

import java.util.List;

public class Clip
{
	public static Clip create(ClipMeta meta, List<ClipMime> mimes,
			List<ClipItem> items)
	{
		Clip clip = new Clip();
		clip.meta = meta;
		clip.mimes = mimes;
		clip.items = items;
		return clip;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof Clip))
		{
			return false;
		}
		Clip rhs = (Clip)obj;
		return meta.equals(rhs.meta) && mimes.equals(rhs.mimes)
				&& items.equals(rhs.items);
	}

	@Embedded
	public ClipMeta meta;
	@Relation(parentColumn = ClipMeta.COL_ID,
			entityColumn = ClipMime.COL_CLIP_ID)
	public List<ClipMime> mimes;
	@Relation(parentColumn = ClipMeta.COL_ID,
			entityColumn = ClipItem.COL_CLIP_ID)
	public List<ClipItem> items;
}
