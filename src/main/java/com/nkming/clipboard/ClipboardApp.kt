package com.nkming.clipboard

import android.app.Application
import android.content.Context
import android.preference.PreferenceManager
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
		initDefaultPref()

		ContextCompat.startForegroundService(context,
				ClipboardService.buildStartIntent(context))
	}

	private fun initLog()
	{
		Log.isShowDebug = BuildConfig.DEBUG
		Log.isShowVerbose = BuildConfig.DEBUG
	}

	private fun initDefaultPref()
	{
		PreferenceManager.setDefaultValues(this, getString(R.string.pref_file),
				Context.MODE_PRIVATE, R.xml.preference, false)
	}
}
