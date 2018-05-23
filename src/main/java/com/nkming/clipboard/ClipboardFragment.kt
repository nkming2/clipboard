package com.nkming.clipboard

import android.Manifest
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.arch.paging.PagedListAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.support.v7.widget.helper.ItemTouchHelper.ACTION_STATE_IDLE
import android.support.v7.widget.helper.ItemTouchHelper.ACTION_STATE_SWIPE
import android.text.TextUtils
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.nkming.clipboard.ClipboardApp.Companion.context
import com.nkming.clipboard.model.room.Clip
import com.nkming.clipboard.model.room.Db
import com.nkming.clipboard.model.room.toClipDataItem
import com.nkming.utils.Log
import com.nkming.utils.app.FragmentEx
import com.nkming.utils.widget.ImageViewEx
import java.io.File
import java.net.URI
import java.text.DateFormat
import java.util.*

class ClipboardFragment : FragmentEx()
{
	companion object
	{
		fun create() = ClipboardFragment()

		private val LOG_TAG = ClipboardFragment::class.java.canonicalName
	}

	class MyViewModel : ViewModel()
	{
		val clipLiveData = Db.loadAllClips()
	}

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
			savedInstanceState: Bundle?): View?
	{
		return inflater.inflate(R.layout.clipboard_frag, container, false)
	}

	override fun onActivityCreated(savedInstanceState: Bundle?)
	{
		super.onActivityCreated(savedInstanceState)
		ensureReadPermission()
		initList()
	}

	override fun onStop()
	{
		super.onStop()
		_dialog?.cancel()
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
	{
		inflater.inflate(R.menu.clipboard, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean
	{
		return when (item.itemId)
		{
			R.id.action_settings -> {
				startActivity(Intent(context, ConfigActivity::class.java))
				true
			}

			R.id.action_clear -> {
				onClearClick()
				true
			}

			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onRequestPermissionsResult(requestCode: Int,
			permissions: Array<out String>, grantResults: IntArray)
	{
		if (requestCode == ClipboardApp.PERMISSION_REQ_READ_EXT_STORAGE)
		{
			if (grantResults.isNotEmpty()
					&& grantResults[0] == PackageManager.PERMISSION_GRANTED)
			{
				Log.i("$LOG_TAG.onRequestPermissionsResult",
						"READ_EXTERNAL_STORAGE granted")
				// Reload the list
				_list.adapter.notifyDataSetChanged()
			}
		}
		else
		{
			Log.e("$LOG_TAG.onRequestPermissionsResult",
					"Unknown req code: $requestCode")
		}
	}

	private fun initList()
	{
		val adapter = MyAdapter(context!!)
		val viewModel = ViewModelProviders.of(this).get(MyViewModel::class.java)
		viewModel.clipLiveData.observe(this, Observer{
			Log.d(LOG_TAG, "Data delivery~~")
			adapter.submitList(it)
		})
		adapter.onCopyListener = ::onCopy
		adapter.onRemoveListener = ::onRemove
		_list.adapter = adapter
		_list.layoutManager = LinearLayoutManager(activity)

		val touchHelper = ItemTouchHelper(object: ItemTouchHelper.Callback()
		{
			override fun getMovementFlags(recyclerView: RecyclerView,
					viewHolder: RecyclerView.ViewHolder): Int
			{
				val idle = makeFlag(ACTION_STATE_IDLE, ItemTouchHelper.LEFT
						or ItemTouchHelper.RIGHT)
				val swipe = makeFlag(ACTION_STATE_SWIPE, ItemTouchHelper.LEFT
						or ItemTouchHelper.RIGHT)
				return idle or swipe
			}

			override fun onMove(recyclerView: RecyclerView?,
					viewHolder: RecyclerView.ViewHolder?,
					target: RecyclerView.ViewHolder?) = false

			override fun onSwiped(viewHolder: RecyclerView.ViewHolder,
					direction: Int)
			{
				adapter.onRemove(viewHolder as MyViewHolder)
			}
		})
		touchHelper.attachToRecyclerView(_list)
	}

	private fun onCopy(clip: Clip)
	{
		val i = ClipboardService.buildReclipIntent(context!!, clip.meta.createAt)
		context!!.startService(i)
	}

	private fun onRemove(clip: Clip)
	{
		Db.deleteClips(clip, onNext = {
			_undoClip = clip

			val snackbar = Snackbar.make(_coordinator, R.string.clip_removed,
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
			Toast.makeText(context, R.string.undo_failed, Toast.LENGTH_LONG)
					.show()
			return
		}
		Db.insertClips(_undoClip!!, onError = {
			Log.e("$LOG_TAG.onUndo", "Failed while insertClips", it)
			Toast.makeText(context, R.string.undo_failed, Toast.LENGTH_LONG)
					.show()
		})
	}

	private fun onClearClick()
	{
		_dialog?.cancel()
		_dialog = AlertDialog.Builder(context!!)
				.setMessage(R.string.dialog_clear_confirm_content)
				.setPositiveButton(android.R.string.ok, {_, _ -> run{
					Db.nukeClips()
				}})
				.setNegativeButton(android.R.string.cancel, null)
				.create()
		_dialog!!.show()
	}

	private fun ensureReadPermission()
	{
		val permissionCheck = ContextCompat.checkSelfPermission(context!!,
				Manifest.permission.READ_EXTERNAL_STORAGE)
		if (permissionCheck != PackageManager.PERMISSION_GRANTED)
		{
			requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
					ClipboardApp.PERMISSION_REQ_READ_EXT_STORAGE)
		}
	}

	private val _coordinator by lazyView<CoordinatorLayout>(R.id.coordinator)
	private val _list by lazyView<RecyclerView>(android.R.id.list)
	private var _dialog: Dialog? = null

	private var _undoClip: Clip? = null
}

private class MyAdapter(context: Context)
		: PagedListAdapter<Clip, MyViewHolder>(DIFF_CALLBACK)
{
	companion object
	{
		private val DIFF_CALLBACK = object: DiffUtil.ItemCallback<Clip>()
		{
			override fun areItemsTheSame(oldItem: Clip, newItem: Clip): Boolean
			{
				return oldItem.meta.id == newItem.meta.id
			}

			override fun areContentsTheSame(oldItem: Clip, newItem: Clip)
					: Boolean
			{
				return oldItem == newItem
			}
		}

		private val LOG_TAG = MyAdapter::class.java.canonicalName
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
			: MyViewHolder
	{
		val v = _inflater.inflate(R.layout.clipboard_frag_item, parent, false)
		return MyViewHolder(v, onCopy = ::onCopy, onRemove = ::onRemove)
	}

	override fun onBindViewHolder(holder: MyViewHolder, position: Int)
	{
		val clip = getItem(position)
		if (clip != null)
		{
			holder.bindTo(clip)
		}
		else
		{
			holder.clear()
		}
	}

	fun onRemove(holder: MyViewHolder)
	{
		Log.i("$LOG_TAG.onRemove", "Position: ${holder.adapterPosition}")
		val clip = getItem(holder.adapterPosition)
		if (clip == null)
		{
			Log.e("$LOG_TAG.onRemove", "clip == null(?!)")
			Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_LONG)
					.show()
			return
		}
		onRemoveListener?.invoke(clip)
	}

	var onCopyListener: ((Clip) -> Unit)? = null
	var onRemoveListener: ((Clip) -> Unit)? = null

	private fun onCopy(holder: MyViewHolder)
	{
		Log.i("$LOG_TAG.onCopy", "Position: ${holder.adapterPosition}")
		val clip = getItem(holder.adapterPosition)
		if (clip == null)
		{
			Log.e("$LOG_TAG.onCopy", "clip == null(?!)")
			Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_LONG)
					.show()
			return
		}
		onCopyListener?.invoke(clip)
	}

	private val _context = context
	private val _inflater = LayoutInflater.from(context)
}

private class MyViewHolder(root: View,
		onCopy: ((holder: MyViewHolder) -> Unit)? = null,
		onRemove: ((holder: MyViewHolder) -> Unit)? = null)
		: RecyclerView.ViewHolder(root)
{
	companion object
	{
		private val LOG_TAG = MyViewHolder::class.java.canonicalName
	}

	fun bindTo(clip: Clip)
	{
		clear()
		val repr = getRepresentation(clip)
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
				Date(clip.meta.createAt))

		_copy.setOnClickListener{_onCopy?.invoke(this)}
		_remove.setOnClickListener{_onRemove?.invoke(this)}
	}

	fun clear()
	{
		_image.setImageDrawable(null)
		_image.visibility = View.GONE
		_text.text = null
		_text.visibility = View.GONE
		_datetime.text = null
		_copy.setOnClickListener(null)
		_remove.setOnClickListener(null)
	}

	private fun getRepresentation(clip: Clip): Pair<String?, Uri?>
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

	private val _onCopy = onCopy
	private val _onRemove = onRemove
	private val _image = root.findViewById<ImageViewEx>(R.id.image)
	private val _text = root.findViewById<TextView>(R.id.text)
	private val _datetime = root.findViewById<TextView>(R.id.datetime)
	private val _copy = root.findViewById<Button>(R.id.copy)
	private val _remove = root.findViewById<Button>(R.id.remove)
}
