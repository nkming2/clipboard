package com.nkming.clipboard

import android.app.Application
import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.preference.PreferenceManager
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

		val pref = Preference.from(this)
		migrateVersion(pref)

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

	private fun migrateVersion(pref: Preference)
	{
		if (pref.lastVersion == BuildConfig.VERSION_CODE)
		{
			// Same version
			return
		}
		else if (pref.lastVersion == -1)
		{
			// New install
		}
		else if (pref.lastVersion < BuildConfig.VERSION_CODE)
		{
			// Upgrade
			// Currently no migration needed
		}
		else if (pref.lastVersion > BuildConfig.VERSION_CODE)
		{
			// Downgrade o.O
		}
		pref.lastVersion = BuildConfig.VERSION_CODE
		pref.commit()
	}
}
