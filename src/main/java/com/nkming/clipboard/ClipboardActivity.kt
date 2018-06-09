package com.nkming.clipboard

import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.widget.Toast
import com.nkming.clipboard.model.room.Clip
import com.nkming.clipboard.model.room.Db
import com.nkming.utils.Log

interface OnRemoveClipListener
{
	fun onRemoveClip(clip: Clip)
}

class ClipboardActivity : BaseActivity(), OnRemoveClipListener
{
	companion object
	{
		private val LOG_TAG = ClipboardActivity::class.java.canonicalName
	}

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(R.layout.frag_activity)
		if (savedInstanceState == null)
		{
			supportFragmentManager.beginTransaction()
					.add(R.id.container, ClipboardFragment.create())
					.commit()
		}
	}

	override fun onRemoveClip(clip: Clip)
	{
		Db.deleteClips(clip, onNext = {
			_undoClip = clip

			val snackbar = Snackbar.make(_container, R.string.clip_removed,
					Snackbar.LENGTH_LONG)
			snackbar.setAction(R.string.undo, {
				onUndo()
			})
			snackbar.show()
		})
	}

	private fun onUndo()
	{
		if (_undoClip == null)
		{
			Log.w("$LOG_TAG.onUndo", "_undoClip == null")
			Toast.makeText(this, R.string.undo_failed, Toast.LENGTH_LONG)
					.show()
			return
		}
		Db.insertClips(_undoClip!!, onError = {
			Log.e("$LOG_TAG.onUndo", "Failed while insertClips", it)
			Toast.makeText(this, R.string.undo_failed, Toast.LENGTH_LONG)
					.show()
		})
	}

	private val _container by lazy{findViewById<CoordinatorLayout>(
			R.id.container)}

	private var _undoClip: Clip? = null
}
