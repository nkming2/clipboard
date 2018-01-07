package com.nkming.clipboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import com.nkming.utils.Log

class BootReceiver : BroadcastReceiver()
{
	companion object
	{
		private val LOG_TAG = BootReceiver::class.java.canonicalName
	}

	override fun onReceive(context: Context, intent: Intent?)
	{
		Log.d(LOG_TAG, "onReceive()")
		ContextCompat.startForegroundService(context,
				ClipboardService.buildStartIntent(context))
	}
}
