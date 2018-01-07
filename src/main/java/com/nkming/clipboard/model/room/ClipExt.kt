package com.nkming.clipboard.model.room

import android.content.ClipData
import android.net.Uri

fun Clip.toClipData(): ClipData
{
	val product = ClipData(meta.label, mimes.map{it.mime}.toTypedArray(),
			items[0].toClipDataItem())
	for (it in items.slice(1 until items.size))
	{
		product.addItem(it.toClipDataItem())
	}
	return product
}

fun ClipItem.toClipDataItem() = ClipData.Item(text, htmlText, null,
		if (uri != null) Uri.parse(uri) else null)
