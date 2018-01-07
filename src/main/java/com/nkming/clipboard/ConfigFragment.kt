package com.nkming.clipboard

import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat

class ConfigFragment : PreferenceFragmentCompat()
{
	companion object
	{
		@JvmStatic
		fun create(): ConfigFragment
		{
			return ConfigFragment()
		}
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?,
			rootKey: String?)
	{
		preferenceManager.sharedPreferencesName = getString(R.string.pref_file)
		setPreferencesFromResource(R.xml.preference, rootKey)
	}

	override fun onActivityCreated(savedInstanceState: Bundle?)
	{
		super.onActivityCreated(savedInstanceState)
		init()
	}

	private fun init()
	{
		initAbout()
	}

	private fun initAbout()
	{
		val aboutVersion = findPreference(getString(R.string.about_version_key))
		aboutVersion.summary = BuildConfig.VERSION_NAME
	}
}
