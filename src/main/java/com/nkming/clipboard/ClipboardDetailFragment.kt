package com.nkming.clipboard

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.nkming.clipboard.model.room.Clip
import com.nkming.clipboard.model.room.ClipItem
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

	override fun onAttach(context: Context?)
	{
		super.onAttach(context)
		if (activity !is OnRemoveClipListener)
		{
			throw IllegalStateException("activity !is OnRemoveClipListener")
		}
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

		_copy.setOnClickListener{onCopy()}
		if (_clip.items.any{!it.text.isNullOrEmpty()
				|| !it.htmlText.isNullOrEmpty()})
		{
			_edit.isEnabled = true
			_edit.setOnClickListener{beginEdit()}
		}
		else
		{
			_edit.isEnabled = false
			_edit.alpha = .38f
		}
		_remove.setOnClickListener{onRemove()}
	}

	private fun onCopy()
	{
		val i = ClipboardService.buildReclipIntent(context!!,
				_clip.meta.createAt)
		context!!.startService(i)
	}

	private fun onRemove()
	{
		_onRemoveClipListener.onRemoveClip(_clip)
		fragmentManager?.popBackStack()
	}

	private fun beginEdit()
	{
		_editText.setText(_text.text)
		_text.visibility = View.GONE
		_editText.visibility = View.VISIBLE
		_editText.post{
			_editText.requestFocus()
			// Show keyboard
			val imm = context!!.getSystemService(Context.INPUT_METHOD_SERVICE)
					as InputMethodManager?
			imm?.showSoftInput(_editText, 0)
		}

		_copy.setOnClickListener(null)
		_edit.setOnClickListener(null)
		_remove.setOnClickListener(null)
		_apply.setOnClickListener{applyEdit()}
		_discard.setOnClickListener{discardEdit()}

		transitButtons(arrayOf(_apply, _discard), arrayOf(_copy, _edit, _remove))
	}

	private fun endEdit()
	{
		_editText.visibility = View.GONE
		_editText.setText("")
		_text.visibility = View.VISIBLE

		// Hide keyboard
		val imm = context!!.getSystemService(Context.INPUT_METHOD_SERVICE)
				as InputMethodManager?
		imm?.hideSoftInputFromWindow(_editText.windowToken, 0)

		_copy.setOnClickListener{onCopy()}
		_edit.setOnClickListener{beginEdit()}
		_remove.setOnClickListener{onRemove()}
		_apply.setOnClickListener(null)
		_discard.setOnClickListener(null)

		transitButtons(arrayOf(_copy, _edit, _remove), arrayOf(_apply, _discard))
	}

	private fun applyEdit()
	{
		persistEdit()
		_text.setText(_editText.text)
		endEdit()
	}

	private fun discardEdit()
	{
		endEdit()
	}

	private fun persistEdit()
	{
		// Clear the old text
		for (item in _clip.items)
		{
			item.text = null
			item.htmlText = null
		}
		val (removes_, items_) = _clip.items.partition{it.text.isNullOrEmpty()
				&& it.htmlText.isNullOrEmpty() && it.uri.isNullOrEmpty()}
		val items = items_.toMutableList()
		val removes = removes_.toMutableList()
		// Add the new text
		var add: ClipItem? = null
		if (removes.isEmpty())
		{
			add = ClipItem(_editText.text.toString(), null, null)
		}
		else
		{
			// Reuse one
			items.add(removes.removeAt(0))
		}
		if (items.isNotEmpty())
		{
			items.last().text = _editText.text.toString()
			// TODO Support HTML text editing?
			items.last().htmlText = null
		}

		Db.transaction({
			// Remove empty items
			if (removes.isNotEmpty())
			{
				Db.instance.clipDao()._deleteClipItems(*removes.toTypedArray())
			}
			// Insert or update the new text
			if (add != null)
			{
				add.clipId = _clip.meta.id
				Db.instance.clipDao()._insertClipItems(add)
			}
			else
			{
				Db.instance.clipDao()._updateClipItems(items.first())
			}
		})
	}

	private fun transitButtons(in_: Array<View>, out: Array<View>)
	{
		val duration = resources.getInteger(
				android.R.integer.config_shortAnimTime).toLong()
		for ((i, v) in out.withIndex())
		{
			v.visibility = View.VISIBLE
			v.scaleX = 1f
			v.scaleY = 1f
			v.animate().scaleX(0f).scaleY(0f)
					.setDuration(duration)
					.setInterpolator(AccelerateInterpolator())
					.setStartDelay(duration * i / 2)
		}
		// Make them gone so they are no longer clickable
		out.last().animate().setListener(object: AnimatorListenerAdapter()
		{
			override fun onAnimationEnd(animation: Animator?)
			{
				if (!isViewDestroyed)
				{
					for (v in out)
					{
						v.visibility = View.GONE
					}
				}
			}
		})

		for ((i, v) in in_.withIndex())
		{
			v.visibility = View.VISIBLE
			v.scaleX = 0f
			v.scaleY = 0f
			v.animate().scaleX(1f).scaleY(1f)
					.setDuration(duration)
					.setInterpolator(DecelerateInterpolator())
					.setStartDelay(duration * (i + out.size) / 2)
					.setListener(null)
		}
	}

	private val _clipCreateAt by lazy{arguments?.getLong(EXTRA_CLIP_CREATE_AT)
			?: 0}
	private lateinit var _clip: Clip
	private val _image by lazyView<ImageViewEx>(R.id.image)
	private val _text by lazyView<TextView>(R.id.text)
	private val _editText by lazyView<EditText>(R.id.edit_text)
	private val _datetime by lazyView<TextView>(R.id.datetime)
	private val _copy by lazyView<ImageButton>(R.id.copy)
	private val _edit by lazyView<ImageButton>(R.id.edit)
	private val _remove by lazyView<ImageButton>(R.id.remove)
	private val _apply by lazyView<ImageButton>(R.id.apply)
	private val _discard by lazyView<ImageButton>(R.id.discard)

	private val _onRemoveClipListener by lazy{activity as OnRemoveClipListener}
}
