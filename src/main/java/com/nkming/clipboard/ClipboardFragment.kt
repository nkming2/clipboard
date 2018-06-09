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
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.support.v7.widget.helper.ItemTouchHelper.ACTION_STATE_IDLE
import android.support.v7.widget.helper.ItemTouchHelper.ACTION_STATE_SWIPE
import android.transition.TransitionInflater
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.nkming.clipboard.ClipboardApp.Companion.context
import com.nkming.clipboard.model.room.Clip
import com.nkming.clipboard.model.room.Db
import com.nkming.utils.Log
import com.nkming.utils.app.FragmentEx
import com.nkming.utils.widget.ImageViewEx
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
		adapter.onExpandListener = ::onExpand
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
		_onRemoveClipListener.onRemoveClip(clip)
	}

	private fun onExpand(clip: Clip, holder: MyViewHolder)
	{
		val f = ClipboardDetailFragment.create(clip.meta.createAt)
		val transaction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			val inflater = TransitionInflater.from(context)
			sharedElementReturnTransition = inflater.inflateTransition(
					android.R.transition.move)
			exitTransition = null

			f.sharedElementEnterTransition = inflater.inflateTransition(
					android.R.transition.move)
			f.enterTransition = null

			val transitionName = context!!.getString(
					R.string.transition_start_card, clip.meta.createAt)
			fragmentManager!!.beginTransaction()
					.addSharedElement(holder.itemView, transitionName)
		}
		else
		{
			fragmentManager!!.beginTransaction()
					.setCustomAnimations(android.R.anim.fade_in,
							android.R.anim.fade_out)
		}
		transaction.hide(this)
				.add(R.id.container, f)
				.addToBackStack(ClipboardDetailFragment.FRAGMENT_STACK_NAME)
				.setReorderingAllowed(true)
				.commitAllowingStateLoss()
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

	private val _list by lazyView<RecyclerView>(android.R.id.list)
	private var _dialog: Dialog? = null

	private val _onRemoveClipListener by lazy{activity as OnRemoveClipListener}
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
		return MyViewHolder(v, onCopy = ::onCopy, onRemove = ::onRemove,
				onExpand = ::onExpand, onClick = ::onCopy)
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
	var onExpandListener: ((Clip, MyViewHolder) -> Unit)? = null

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

	private fun onExpand(holder: MyViewHolder)
	{
		Log.i("$LOG_TAG.onExpand", "Position: ${holder.adapterPosition}")
		val clip = getItem(holder.adapterPosition)
		if (clip == null)
		{
			Log.e("$LOG_TAG.onExpand", "clip == null(?!)")
			Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_LONG)
					.show()
			return
		}
		onExpandListener?.invoke(clip, holder)
	}

	private val _context = context
	private val _inflater = LayoutInflater.from(context)
}

private class MyViewHolder(root: View,
		onCopy: ((holder: MyViewHolder) -> Unit)? = null,
		onRemove: ((holder: MyViewHolder) -> Unit)? = null,
		onExpand: ((holder: MyViewHolder) -> Unit)? = null,
		onClick: ((holder: MyViewHolder) -> Unit)? = null)
		: RecyclerView.ViewHolder(root)
{
	companion object
	{
		private val LOG_TAG = MyViewHolder::class.java.canonicalName
	}

	fun bindTo(clip: Clip)
	{
		clear()
		val repr = ClipUtil.getRepresentation(context, clip)
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

		itemView.setOnClickListener{_onClick?.invoke(this)}
		_copy.setOnClickListener{_onCopy?.invoke(this)}
		_remove.setOnClickListener{_onRemove?.invoke(this)}
		_more.setOnClickListener{_onExpand?.invoke(this)}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			val transitionName = context.getString(
					R.string.transition_start_card, clip.meta.createAt)
			itemView.transitionName = transitionName
		}
	}

	fun clear()
	{
		_image.setImageDrawable(null)
		_image.visibility = View.GONE
		_text.text = null
		_text.visibility = View.GONE
		_datetime.text = null
		itemView.setOnClickListener(null)
		_copy.setOnClickListener(null)
		_remove.setOnClickListener(null)
		_more.setOnClickListener(null)
	}

	private val _onCopy = onCopy
	private val _onRemove = onRemove
	private val _onExpand = onExpand
	private val _onClick = onClick
	private val _image = root.findViewById<ImageViewEx>(R.id.image)
	private val _text = root.findViewById<TextView>(R.id.text)
	private val _datetime = root.findViewById<TextView>(R.id.datetime)
	private val _copy = root.findViewById<Button>(R.id.copy)
	private val _remove = root.findViewById<Button>(R.id.remove)
	private val _more = root.findViewById<ImageButton>(R.id.more)
}
