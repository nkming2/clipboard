package com.nkming.clipboard

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import com.nkming.clipboard.model.room.Clip
import com.nkming.clipboard.model.room.toClipDataItem
import java.io.File
import java.net.URI

object ClipUtil
{
	fun getRepresentation(context: Context, clip: Clip): Pair<String?, Uri?>
	{
		// Favor displaying plain text over htmltext
		val text = clip.items.firstOrNull{!TextUtils.isEmpty(it.text)}
				?: clip.items.firstOrNull{!TextUtils.isEmpty(it.htmlText)}
		if (text != null)
		{
			return Pair(text.text, null)
		}

		val uriItems = clip.items.filter{!TextUtils.isEmpty(it.uri)}
		if (clip.mimes.any{it.mime.startsWith("image", ignoreCase = true)})
		{
			val resolver = context.contentResolver
			for (item in uriItems)
			{
				val uri = Uri.parse(item.uri)
				// getType can return null if uri is not a content uri
				if (resolver.getType(uri)?.startsWith("image", ignoreCase = true)
						== true)
				{
					return Pair(null, uri)
				}
				else if (item.uri.startsWith("file://"))
				{
					// File uri pointing to an image
					val f = File(URI.create(item.uri))
					if (f.exists())
					{
						return Pair(null, uri)
					}
				}
			}
		}
		for (item in uriItems)
		{
			val repr = item.toClipDataItem().coerceToText(context).toString()
			if (!TextUtils.isEmpty(repr))
			{
				return Pair(repr, null)
			}
		}
		return Pair(context.getString(R.string.notif_content_non_text), null)
	}
}
