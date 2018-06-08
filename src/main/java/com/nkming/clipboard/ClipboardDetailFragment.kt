package com.nkming.clipboard

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.nkming.clipboard.model.room.Clip
import com.nkming.clipboard.model.room.Db
import com.nkming.utils.app.FragmentEx
import com.nkming.utils.widget.ImageViewEx
import java.text.DateFormat
import java.util.*

class ClipboardDetailFragment : FragmentEx()
{
	companion object
	{
		fun create(clipCreateAt: Long): ClipboardDetailFragment
		{
			val args = Bundle()
			args.putLong(EXTRA_CLIP_CREATE_AT, clipCreateAt)
			val f = ClipboardDetailFragment()
			f.arguments = args
			return f
		}

		const val FRAGMENT_STACK_NAME = "clip_detail"

		private const val EXTRA_CLIP_CREATE_AT = "clipCreateAt"
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
			savedInstanceState: Bundle?): View
	{
		val v = inflater.inflate(R.layout.clipboard_frag_detail, container,
				false)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			val transitionName = context!!.getString(
					R.string.transition_start_card, _clipCreateAt)
			v.transitionName = transitionName
		}
		return v
	}

	override fun onActivityCreated(savedInstanceState: Bundle?)
	{
		super.onActivityCreated(savedInstanceState)
		_text.isClickable = false
		_text.isFocusable = false
		Db.loadClipByCreateAt(_clipCreateAt, onNext = {
			if (!isViewDestroyed)
			{
				_clip = it
				initView()
			}
		}, onError = {
			if (!isViewDestroyed)
			{
				fragmentManager!!.beginTransaction()
						.remove(this)
						.commitAllowingStateLoss()
			}
			Toast.makeText(context, R.string.clipboard_detail_query_failed,
					Toast.LENGTH_LONG).show()
		})
	}

	override fun onBackPressed(): Boolean
	{
		super.onBackPressed()
		fragmentManager?.popBackStack()
		return true
	}

	private fun initView()
	{
		val repr = ClipUtil.getRepresentation(context!!, _clip)
		if (repr.first != null)
		{
			_text.visibility = View.VISIBLE
			_text.text = repr.first
		}
		if (repr.second != null)
		{
			_image.visibility = View.VISIBLE
			_image.setImageURI(repr.second)
		}
		_datetime.text = DateFormat.getDateTimeInstance().format(
				Date(_clip.meta.createAt))
	}

	private val _clipCreateAt by lazy{arguments?.getLong(EXTRA_CLIP_CREATE_AT)
			?: 0}
	private lateinit var _clip: Clip
	private val _image by lazyView<ImageViewEx>(R.id.image)
	private val _text by lazyView<TextView>(R.id.text)
	private val _datetime by lazyView<TextView>(R.id.datetime)
}
