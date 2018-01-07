package com.nkming.clipboard.model.room;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity(tableName = ClipItem.TABLE,
		foreignKeys = @ForeignKey(entity = ClipMeta.class,
				parentColumns = ClipMeta.COL_ID,
				childColumns = ClipItem.COL_CLIP_ID, onDelete = CASCADE))
public class ClipItem
{
	public static final String TABLE = "clip_items";
	public static final String COL_ID = "id";
	public static final String COL_CLIP_ID = "clip_id";
	public static final String COL_TEXT = "text";
	public static final String COL_HTML_TEXT = "html_text";
	public static final String COL_CLIP_URI = "uri";

	public ClipItem(String text, String htmlText, String uri)
	{
		this.text = text;
		this.htmlText = htmlText;
		this.uri = uri;
	}

	@PrimaryKey(autoGenerate = true)
	public long id;

	@ColumnInfo(name = COL_CLIP_ID, index = true)
	public long clipId;

	public String text;

	@ColumnInfo(name = "html_text")
	public String htmlText;

	public String uri;
}
