package com.nkming.clipboard

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Preference(context: Context, pref: SharedPreferences)
{
	companion object
	{
		@JvmStatic
		fun from(context: Context): Preference
		{
			return Preference(context, context.getSharedPreferences(
					context.getString(R.string.pref_file), Context.MODE_PRIVATE))
		}
	}

	fun commit(): Boolean
	{
		return _editLock.withLock(
		{
			if (_edit.commit())
			{
				__edit = null
				return true
			}
			else
			{
				return false
			}
		})
	}

	fun apply()
	{
		_editLock.withLock(
		{
			_edit.apply()
			__edit = null
		})
	}

	var onSharedPreferenceChangeListener
			: SharedPreferences.OnSharedPreferenceChangeListener? = null
		set(v)
		{
			if (field != null)
			{
				_pref.unregisterOnSharedPreferenceChangeListener(field)
			}
			if (v != null)
			{
				_pref.registerOnSharedPreferenceChangeListener(v)
				field = v
			}
		}

	var lastVersion: Int
		get()
		{
			return _pref.getInt(_lastVersionKey, -1)
		}
		set(v)
		{
			_edit.putInt(_lastVersionKey, v)
		}

	var isDarkTheme: Boolean
		get()
		{
			return _pref.getBoolean(_darkThemeKey, false)
		}
		set(v)
		{
			_edit.putBoolean(_darkThemeKey, v)
		}

	private val _lastVersionKey by lazy{_context.getString(
			R.string.pref_last_version_key)}
	private val _darkThemeKey by lazy{_context.getString(
			R.string.pref_dark_theme_key)}

	private val _context = context
	private val _pref = pref
	private val _edit: SharedPreferences.Editor
		get()
		{
			_editLock.withLock(
			{
				if (__edit == null)
				{
					__edit = _pref.edit()
				}
				return __edit!!
			})
		}
	private var __edit: SharedPreferences.Editor? = null
	private val _editLock = ReentrantLock(true)
}
