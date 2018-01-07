package com.nkming.clipboard

import android.app.Application
import android.content.Context
import android.support.v4.content.ContextCompat
import com.nkming.utils.Log

class ClipboardApp : Application()
{
	companion object
	{
		lateinit var context: Context
	}

	override fun onCreate()
	{
		super.onCreate()
		context = applicationContext
		initLog()

		ContextCompat.startForegroundService(context,
				ClipboardService.buildStartIntent(context))
	}

	private fun initLog()
	{
		Log.isShowDebug = BuildConfig.DEBUG
		Log.isShowVerbose = BuildConfig.DEBUG
	}
}
