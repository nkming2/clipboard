package com.nkming.clipboard

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.nkming.utils.Log

abstract class ComponentService : Service()
{
	companion object
	{
		private val LOG_TAG = ComponentService::class.java.canonicalName
	}

	override fun onBind(intent: Intent?): IBinder? = null

	override fun onCreate()
	{
		Log.d(LOG_TAG, "onCreate()")
		super.onCreate()
		_components.forEach{it.onCreate()}
	}

	/**
	 * By default START_STICKY is returned. If you want to return something else,
	 * you could override this method but super must be called
	 */
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
	{
		Log.d(LOG_TAG, "onStartCommand()")
		if (!_hasInit)
		{
			_hasInit = true
			onInit()
		}
		_components.forEach{it.onStartCommand(intent, flags, startId)}
		return START_STICKY
	}

	open fun onInit()
	{
		_components.forEach{it.onInit()}
	}

	override fun onDestroy()
	{
		Log.d(LOG_TAG, "onDestroy()")
		super.onDestroy()
		_components.forEach{it.onDestroy()}
	}

	protected abstract val _components: List<ServiceComponent>

	private var _hasInit = false
}

interface ServiceComponent
{
	fun onCreate() {}
	fun onStartCommand(intent: Intent?, flags: Int, startId: Int) {}
	/**
	 * onInit is like onCreate(), but invoked in onStartCommand()
	 */
	fun onInit() {}
	fun onDestroy() {}
}
