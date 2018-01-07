package com.nkming.clipboard

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import com.nkming.utils.app.AppCompatActivityEx

open class BaseActivity : AppCompatActivityEx()
{
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setTheme(_theme)
		_pref.onSharedPreferenceChangeListener =
				SharedPreferences.OnSharedPreferenceChangeListener{_, key -> run{
					if (key == getString(R.string.pref_dark_theme_key))
					{
						if (_isResumed)
						{
							recreate()
						}
						else
						{
							_isRecreatePending = true
						}
					}
				}}
	}

	override fun onResume()
	{
		super.onResume()
		_isResumed = true
		if (_isRecreatePending)
		{
			_handler.post{
				// Why isn't recreate() working? :/
				startActivity(Intent(this, javaClass))
				finish()
			}
		}
	}

	override fun onPause()
	{
		super.onPause()
		_isResumed = false
	}

	override fun onDestroy()
	{
		super.onDestroy()
		_pref.onSharedPreferenceChangeListener = null
	}

	protected open val _hasActionBar = true

	private val _theme: Int
		get()
		{
			val t = if (_pref.isDarkTheme)
			{
				if (_hasActionBar) R.style.AppTheme_Dark else
						R.style.AppTheme_Dark_NoActionBar
			}
			else
			{
				if (_hasActionBar) R.style.AppTheme_Light else
						R.style.AppTheme_Light_NoActionBar
			}
			return t
		}

	private val _pref by lazy{Preference.from(this)}
	private val _handler = Handler()
	private var _isResumed = false
	private var _isRecreatePending = false
}
