package com.nkming.clipboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.preference.Preference
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

		if (getString(R.string.about_translator_credit).isEmpty())
		{
			val aboutTranslator = findPreference(getString(
					R.string.about_translator_key))
			aboutTranslator.summary = getString(R.string.about_translator_help)
			aboutTranslator.onPreferenceClickListener =
					Preference.OnPreferenceClickListener{
						val i = Intent(Intent.ACTION_VIEW)
						i.data = Uri.parse(getString(
								R.string.about_translator_help_url))
						startActivity(i)
						return@OnPreferenceClickListener true
					}
		}
	}
}
