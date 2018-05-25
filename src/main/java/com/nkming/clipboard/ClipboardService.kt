package com.nkming.clipboard

import android.annotation.TargetApi
import android.app.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.media.app.NotificationCompat.MediaStyle
import com.nkming.clipboard.model.room.Clip
import com.nkming.clipboard.model.room.Db
import com.nkming.clipboard.model.room.toClipData
import com.nkming.utils.Log
import com.nkming.utils.type.ext.parseStyledResourceId
import io.reactivex.disposables.Disposable

class ClipboardService : ComponentService()
{
	companion object
	{
		@JvmStatic
		fun buildStartIntent(context: Context) = Intent(context,
				ClipboardService::class.java)

		@JvmStatic
		fun buildReclipIntent(context: Context, clipCreateAt: Long) =
				ClipboardComponent.buildReclipIntent(context, clipCreateAt)
	}

	override val _components: List<ServiceComponent> by lazy{listOf(
			ClipboardComponent(this), NotifComponent(this))}
}

/**
 * Clipboard service component that listen to system clipboard changes and
 * insert to the DB accordingly
 */
private class ClipboardComponent(context: Context) : ServiceComponent
{
	companion object
	{
		fun buildReclipIntent(context: Context, clipCreateAt: Long): Intent
		{
			val i = Intent(context, ClipboardService::class.java)
			i.action = ACTION_RECLIP
			i.putExtra(EXTRA_CLIP_CREATE_AT, clipCreateAt)
			return i
		}

		private val LOG_TAG = ClipboardComponent::class.java.canonicalName
		private val ACTION_RECLIP = "$LOG_TAG.ACTION_RECLIP"
		private val EXTRA_CLIP_CREATE_AT = "$LOG_TAG.EXTRA_CLIP_CREATE_AT"
	}

	override fun onCreate()
	{
		_clipboard.addPrimaryClipChangedListener(_onPrimaryClipChanged)
	}

	override fun onDestroy()
	{
		_clipboard.removePrimaryClipChangedListener(_onPrimaryClipChanged)
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int)
	{
		Log.d(LOG_TAG, "onStartCommand()")
		when (intent?.action)
		{
			ACTION_RECLIP -> reclip(intent)
		}
	}

	private fun insertClip(clipData: ClipData)
	{
		val clip = clipData.toClip()
		// We currently don't support intent clips
		if (clip.items.isNotEmpty())
		{
			Db.insertClips(clip, onError = {
				Log.e("$LOG_TAG.insertClip", "Failed while insert", it)
			})
		}
	}

	private fun reclip(data: Intent)
	{
		Log.d(LOG_TAG, "reclip()")
		assert(data.hasExtra(EXTRA_CLIP_CREATE_AT))
		val clipCreateAt = data.getLongExtra(EXTRA_CLIP_CREATE_AT, 0)
		Db.loadClipByCreateAt(clipCreateAt, onNext = {
			it.meta.createAt = System.currentTimeMillis()
			Db.updateClips(it, onNext = {
				_clipboard.removePrimaryClipChangedListener(
						_onPrimaryClipChanged)
				val clipData = it.toClipData()
				_clipboard.primaryClip = clipData
				_clipboard.addPrimaryClipChangedListener(_onPrimaryClipChanged)
			})
		})
	}

	private val _onPrimaryClipChanged =
			ClipboardManager.OnPrimaryClipChangedListener{
		val clip = _clipboard.primaryClip ?: return@OnPrimaryClipChangedListener
		if (clip.isNull())
		{
			Log.d("$LOG_TAG._onPrimaryClipChanged", "Skipped null clip")
			return@OnPrimaryClipChangedListener
		}
		val now = System.currentTimeMillis()
		try
		{
			// For some unknown reason, Chrome sometimes put multiple identical
			// clips to the manager
			if (Math.abs(now - _prevTime) < 250 && equals(clip, _prevClip))
			{
				Log.d("$LOG_TAG._onPrimaryClipChanged", "Skipped duplicated clip")
				return@OnPrimaryClipChangedListener
			}
			insertClip(clip)
		}
		finally
		{
			_prevTime = now
			_prevClip = clip
		}
	}

	private val _clipboard by lazy{context.getSystemService(
			Context.CLIPBOARD_SERVICE) as ClipboardManager}

	private var _prevClip: ClipData? = null
	private var _prevTime: Long = 0
}

private class NotifComponent(service: Service) : ServiceComponent
{
	companion object
	{
		fun buildReclipPrevIntent(context: Context): Intent
		{
			val i = Intent(context, ClipboardService::class.java)
			i.action = ACTION_RECLIP_PREV
			return i
		}

		private const val NOTIF_ID = 1
		private val LOG_TAG = NotifComponent::class.java.canonicalName
		private val ACTION_RECLIP_PREV = "$LOG_TAG.ACTION_RECLIP_PREV"
		private val CHANNEL_ID = "clipboard_content"
	}

	override fun onInit()
	{
		Log.d(LOG_TAG, "onInit()")
		_service.startForeground(NOTIF_ID, buildInitialNotification())
		_subscription = Db.observeClipByLatest(onNext = _onClipUpdateLatest,
				onError = {
					Log.e(LOG_TAG, "Failed while observeClipByLatest", it)
				})

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			initNotifChannel()
		}
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int)
	{
		Log.d(LOG_TAG, "onStartCommand()")
		when (intent?.action)
		{
			ACTION_RECLIP_PREV -> onReclipPrev()
		}
	}

	override fun onDestroy()
	{
		super.onDestroy()
		_subscription.dispose()
		_service.stopForeground(true)
	}

	@TargetApi(Build.VERSION_CODES.O)
	private fun initNotifChannel()
	{
		val ch = NotificationChannel(CHANNEL_ID,
				_context.getString(R.string.notif_content_channel_name),
				NotificationManager.IMPORTANCE_MIN)
		ch.description = _context.getString(
				R.string.notif_content_channel_description)
		ch.lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
		_notifManager.createNotificationChannel(ch)
	}

	private fun onReclipPrev()
	{
		Log.d(LOG_TAG, "onReclipPrev()")
		Db.loadClipByLatest(onNext = {
			if (it.isEmpty())
			{
				// TODO Do sth
				return@loadClipByLatest
			}
			val intent = ClipboardComponent.buildReclipIntent(_context,
					it.first().meta.createAt)
			_context.startService(intent)
		}, offset = 1)
	}

	private fun buildInitialNotification(): Notification
	{
		return _notifBuilder.buildEmpty(CHANNEL_ID)
	}

	private fun updateNotification(clip: Clip)
	{
		val n = _notifBuilder.build(CHANNEL_ID, clip, true)
		_notifManager.notify(NOTIF_ID, n)
	}

	private val _onClipUpdateLatest: (Clip) -> Unit = {
		Log.d(LOG_TAG, "_onClipUpdateLatest()")
		updateNotification(it)
	}

	private val _service = service
	private val _context: Context
		get() = _service
	private lateinit var _subscription: Disposable

	private val _notifManager by lazy{_context.getSystemService(
			Context.NOTIFICATION_SERVICE) as NotificationManager}
	private val _notifBuilder by lazy{NotifBuilder(_context)}
}

private class NotifBuilder(context: Context)
{
	companion object
	{
		private val LOG_TAG = NotifBuilder::class.java.canonicalName
	}

	fun build(channelId: String, clip: Clip, hasReclip: Boolean): Notification
	{
		val text: CharSequence? = clip.items.firstOrNull{!it.text.isNullOrEmpty()}
				?.text
		return if (text == null)
		{
			build(channelId, _context.getString(R.string.notif_content_non_text),
					hasReclip)
		}
		else
		{
			build(channelId, text.toString(), hasReclip)
		}
	}

	fun buildEmpty(channelId: String): Notification
	{
		return build(channelId, _context.getString(R.string.notif_content_empty),
				false)
	}

	private fun build(channelId: String, content: String, hasReclip: Boolean)
			: Notification
	{
		val builder = NotificationCompat.Builder(_context, channelId)
		builder.setContentTitle(_context.getString(R.string.notif_title))
				.setContentText(content)
				.setWhen(System.currentTimeMillis())
				.setLocalOnly(true)
				.setOnlyAlertOnce(true)
				.setOngoing(true)
				.setSmallIcon(R.drawable.ic_content_paste_white_24dp)
				.setColor(_notificationColor)

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
		{
			builder.priority = NotificationCompat.PRIORITY_MIN
		}

		val appIntent = Intent(_context, ClipboardActivity::class.java)
		builder.setContentIntent(PendingIntent.getActivity(_context, 0,
				appIntent, PendingIntent.FLAG_UPDATE_CURRENT))

		val style = android.support.v4.app.NotificationCompat.BigTextStyle()
				.bigText(content)
		builder.setStyle(style)

		if (hasReclip)
		{
			val intent = NotifComponent.buildReclipPrevIntent(_context)
			val pendingIntent = PendingIntent.getService(_context, 0, intent,
					PendingIntent.FLAG_UPDATE_CURRENT)
			builder.addAction(R.drawable.ic_restore_white_24dp,
					_context.getString(R.string.notif_action_previous),
					pendingIntent)
			builder.setStyle(MediaStyle().setShowActionsInCompactView(0))
		}
		return builder.build()
	}

	private val _notificationColor by lazy{
		return@lazy try
		{
			val colorId = _context.parseStyledResourceId(R.attr.colorPrimary,
					R.color.primary_dark)
			ContextCompat.getColor(_context, colorId)
		}
		catch (e: Exception)
		{
			Log.e("$LOG_TAG._notificationColor", "Failed while getResourceId", e)
			ContextCompat.getColor(_context, R.color.primary_dark)
		}
	}

	private val _context = context
}
