package com.nkming.clipboard

import android.content.ClipData
import android.content.ClipDescription
import com.nkming.clipboard.model.room.Clip
import com.nkming.clipboard.model.room.ClipItem
import com.nkming.clipboard.model.room.ClipMeta
import com.nkming.clipboard.model.room.ClipMime
import java.util.*

fun ClipData.isNull(): Boolean
{
	return !itemIterable.any{
		it.htmlText ?: it.intent ?: it.text ?: it.uri ?: return@any false
		return@any true
	}
}

fun ClipData.toClip(): Clip
{
	val mimes = description.mimeIterable.map{ClipMime(it)}
	val items = itemIterable.filter{
		it.text != null || it.htmlText != null || it.uri != null
	}.map{
		ClipItem(it.text?.toString(), it.htmlText,
				it.uri?.toString())
	}
	val meta = ClipMeta(System.currentTimeMillis(), description.label?.toString())
	return Clip.create(meta, mimes, items)
}

fun equals(lhs: ClipData?, rhs: ClipData?): Boolean
{
	if (lhs === rhs)
	{
		return true
	}
	lhs ?: return false
	rhs ?: return false
	if (lhs.description.mimeTypeCount != rhs.description.mimeTypeCount)
	{
		return false
	}
	if (lhs.description.mimeIterable.zip(rhs.description.mimeIterable).any{
			it.first != it.second})
	{
		return false
	}
	if (lhs.description.label != rhs.description.label)
	{
		return false
	}
	if (lhs.itemCount != rhs.itemCount)
	{
		return false
	}
	return lhs.itemIterable.zip(rhs.itemIterable).all{
		val l = it.first
		val r = it.second
		return@all (l.htmlText == r.htmlText && l.intent == r.intent
				&& l.text == r.text && l.uri == r.uri)
	}
}

val ClipData.itemIterable: Iterable<ClipData.Item>
	get()
	{
		val it = object: Iterator<ClipData.Item>
		{
			override fun hasNext(): Boolean
			{
				return (itemCount > _i)
			}

			override fun next(): ClipData.Item
			{
				synchronized(_i)
				{
					if (!hasNext())
					{
						throw NoSuchElementException()
					}
					return getItemAt(_i++)
				}
			}

			private var _i = 0
		}
		return object: Iterable<ClipData.Item>
		{
			override fun iterator(): Iterator<ClipData.Item>
			{
				return it
			}
		}
	}

val ClipDescription.mimeIterable: Iterable<String>
	get()
	{
		val it = object: Iterator<String>
		{
			override fun hasNext(): Boolean
			{
				return (mimeTypeCount > _i)
			}

			override fun next(): String
			{
				synchronized(_i)
				{
					if (!hasNext())
					{
						throw NoSuchElementException()
					}
					return getMimeType(_i++)
				}
			}

			private var _i = 0
		}
		return object: Iterable<String>
		{
			override fun iterator(): Iterator<String>
			{
				return it
			}
		}
	}
