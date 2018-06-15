package com.nkming.clipboard

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import com.nkming.utils.app.FragmentEx
import com.nkming.utils.unit.DimensionUtils

abstract class IntroFragment : FragmentEx()
{
	override fun onDestroyView()
	{
		super.onDestroyView()
		_handler.removeCallbacksAndMessages(null)
	}

	protected fun startEnterTransition(views: List<View>,
			endListener: () -> Unit, endDelay: Long = 0)
	{
		for ((i, v) in views.withIndex())
		{
			v.translationX = _xAmplitude
			v.alpha = 0f
			_handler.postDelayed({
				v.animate().translationX(0f).alpha(1f)
						.setDuration(_duration)
						.setInterpolator(DecelerateInterpolator())
			}, _duration / 2 * i)
		}
		_handler.postDelayed(endListener,
				_duration / 2 * (views.size + 1) + endDelay)
	}

	protected fun startExitTransition(views: List<View>,
			endListener: () -> Unit, endDelay: Long = 0)
	{
		for ((i, v) in views.withIndex())
		{
			v.translationX = 0f
			v.alpha = 1f
			_handler.postDelayed(
			{
				v.animate().translationX(-_xAmplitude).alpha(0f)
						.setDuration(_duration)
						.setInterpolator(AccelerateInterpolator())
			}, _duration / 2 * i)
		}
		_handler.postDelayed(endListener,
				_duration / 2 * (views.size + 1) + endDelay)
	}

	protected fun startEnterReverseTransition(views: List<View>,
			endListener: () -> Unit, endDelay: Long = 0)
	{
		for ((i, v) in views.withIndex())
		{
			v.translationX = 0f
			v.alpha = 1f
			_handler.postDelayed(
			{
				v.animate().translationX(_xAmplitude).alpha(0f)
						.setDuration(_duration)
						.setInterpolator(AccelerateInterpolator())
			}, _duration / 2 * i)
		}
		_handler.postDelayed(endListener,
				_duration / 2 * (views.size + 1) + endDelay)
	}

	protected fun startExitReverseTransition(views: List<View>,
			endListener: () -> Unit, endDelay: Long = 0)
	{
		for ((i, v) in views.withIndex())
		{
			v.translationX = -_xAmplitude
			v.alpha = 0f
			_handler.postDelayed(
			{
				v.animate().translationX(0f).alpha(1f)
						.setDuration(_duration)
						.setInterpolator(DecelerateInterpolator())
			}, _duration / 2 * i)
		}
		_handler.postDelayed(endListener,
				_duration / 2 * (views.size + 1) + endDelay)
	}

	private val _handler = Handler()
	private val _xAmplitude by lazy{DimensionUtils.dpToPx(context!!, 80f)}
	private val _duration by lazy{resources.getInteger(
			android.R.integer.config_shortAnimTime).toLong()}
}

class IntroGreetingFragment : IntroFragment()
{
	companion object
	{
		fun create(): IntroGreetingFragment
		{
			return IntroGreetingFragment()
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
			savedInstanceState: Bundle?): View
	{
		super.onCreateView(inflater, container, savedInstanceState)
		return inflater.inflate(R.layout.intro_greeting_frag, container, false)
	}

	override fun onActivityCreated(savedInstanceState: Bundle?)
	{
		super.onActivityCreated(savedInstanceState)
		val callback = {
			if (!isViewDestroyed)
			{
				enableInputs()
			}
		}
		if (isRestoreFromBackstack)
		{
			startExitReverseTransition(_transitViews, callback)
		}
		else
		{
			enableInputs()
		}
	}

	private fun onNextClick()
	{
		disableInputs()
		startExitTransition(_transitViews, {
			val f = IntroThemeFragment.create()
			fragmentManager!!.beginTransaction()
					.replace(R.id.container, f)
					.addToBackStack(null)
					.commitAllowingStateLoss()
		}, resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
	}

	private fun enableInputs()
	{
		_next.setOnClickListener{onNextClick()}
	}

	private fun disableInputs()
	{
		_next.setOnClickListener(null)
	}

	private val _next by lazyView<View>(R.id.next)
	private val _transitViews by viewAwareLazy{
		listOf(findView<View>(R.id.message),
				findView(R.id.detail_message))
	}
}

class IntroThemeFragment : IntroFragment()
{
	companion object
	{
		fun create(): IntroThemeFragment
		{
			return IntroThemeFragment()
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
			savedInstanceState: Bundle?): View
	{
		super.onCreateView(inflater, container, savedInstanceState)
		return inflater.inflate(R.layout.intro_theme_frag, container, false)
	}

	override fun onActivityCreated(savedInstanceState: Bundle?)
	{
		super.onActivityCreated(savedInstanceState)
		val callback = {
			if (!isViewDestroyed)
			{
				enableInputs()
			}
		}
		if (isRestoreFromBackstack)
		{
			startExitReverseTransition(_transitViews, callback)
		}
		else
		{
			startEnterTransition(_transitViews, callback)
		}

		val active = if (_pref.isDarkTheme) _dark else _light
		active.setBackgroundColor(ContextCompat.getColor(context!!,
				R.color.md_grey_500))
	}

	override fun onBackPressed(): Boolean
	{
		if (!super.onBackPressed())
		{
			startEnterReverseTransition(_transitViews, {
				fragmentManager!!.popBackStack()
			}, 100L)
		}
		return true
	}

	private fun onNextClick()
	{
		disableInputs()
		startExitTransition(_transitViews, {
			val f = IntroReclipFragment.create()
			fragmentManager!!.beginTransaction()
					.replace(R.id.container, f)
					.addToBackStack(null)
					.commitAllowingStateLoss()
		}, resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
	}

	private fun enableInputs()
	{
		_next.setOnClickListener{onNextClick()}
		_light.setOnClickListener{
			if (_pref.isDarkTheme)
			{
				_pref.isDarkTheme = false
				_pref.apply()
				// Will auto recreate via BaseActivity
			}
		}
		_dark.setOnClickListener{
			if (!_pref.isDarkTheme)
			{
				_pref.isDarkTheme = true
				_pref.apply()
				// Will auto recreate via BaseActivity
			}
		}
	}

	private fun disableInputs()
	{
		_next.setOnClickListener(null)
		_light.setOnClickListener(null)
		_dark.setOnClickListener(null)
	}

	private val _pref by lazy{Preference.from(context!!)}
	private val _next by lazyView<View>(R.id.next)
	private val _light by lazyView<View>(R.id.light)
	private val _dark by lazyView<View>(R.id.dark)
	private val _transitViews by viewAwareLazy{
		listOf(_light,
				_dark)
	}
}

class IntroReclipFragment : IntroFragment()
{
	companion object
	{
		fun create(): IntroReclipFragment
		{
			return IntroReclipFragment()
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
			savedInstanceState: Bundle?): View
	{
		super.onCreateView(inflater, container, savedInstanceState)
		return inflater.inflate(R.layout.intro_reclip_frag, container, false)
	}

	override fun onActivityCreated(savedInstanceState: Bundle?)
	{
		super.onActivityCreated(savedInstanceState)
		val callback = {
			if (!isViewDestroyed)
			{
				enableInputs()
				_handler.postDelayed(::startAnimation, 800)
			}
		}
		if (isRestoreFromBackstack)
		{
			startExitReverseTransition(_transitViews, callback)
		}
		else
		{
			startEnterTransition(_transitViews, callback)
		}
	}

	override fun onDestroyView()
	{
		super.onDestroyView()
		_handler.removeCallbacksAndMessages(null)
	}

	override fun onBackPressed(): Boolean
	{
		if (!super.onBackPressed())
		{
			startEnterReverseTransition(_transitViews, {
				fragmentManager!!.popBackStack()
			}, 100L)
		}
		return true
	}

	private fun onNextClick()
	{
		disableInputs()
		startExitTransition(_transitViews, {
			val f = IntroRemoveFragment.create()
			fragmentManager!!.beginTransaction()
					.replace(R.id.container, f)
					.addToBackStack(null)
					.commitAllowingStateLoss()
		}, resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
	}

	private fun enableInputs()
	{
		_next.setOnClickListener{onNextClick()}
	}

	private fun disableInputs()
	{
		_next.setOnClickListener(null)
	}

	private fun startAnimation()
	{
		if (!isViewDestroyed)
		{
			_touch.visibility = View.VISIBLE
			_touch.alpha = 1f
			_touch.animate().alpha(0f)
					.setInterpolator(AccelerateInterpolator())
					.setDuration(resources.getInteger(
							android.R.integer.config_longAnimTime).toLong())
			_handler.postDelayed(::startAnimation, 3400)
		}
	}

	private val _handler = Handler()
	private val _next by lazyView<View>(R.id.next)
	private val _touch by lazyView<View>(R.id.touch)
	private val _transitViews by viewAwareLazy{
		listOf(findView<View>(R.id.clip),
				findView(R.id.message))
	}
}

class IntroRemoveFragment : IntroFragment()
{
	companion object
	{
		fun create(): IntroRemoveFragment
		{
			return IntroRemoveFragment()
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
			savedInstanceState: Bundle?): View
	{
		super.onCreateView(inflater, container, savedInstanceState)
		return inflater.inflate(R.layout.intro_remove_frag, container, false)
	}

	override fun onActivityCreated(savedInstanceState: Bundle?)
	{
		super.onActivityCreated(savedInstanceState)
		val callback = {
			if (!isViewDestroyed)
			{
				enableInputs()
				_handler.postDelayed(::startAnimation, 800)
			}
		}
		if (isRestoreFromBackstack)
		{
			startExitReverseTransition(_transitViews, callback)
		}
		else
		{
			startEnterTransition(_transitViews, callback)
		}
	}

	override fun onDestroyView()
	{
		super.onDestroyView()
		_handler.removeCallbacksAndMessages(null)
	}

	override fun onBackPressed(): Boolean
	{
		if (!super.onBackPressed())
		{
			startEnterReverseTransition(_transitViews, {
				fragmentManager!!.popBackStack()
			}, 100L)
		}
		return true
	}

	private fun onNextClick()
	{
		disableInputs()
		startExitTransition(_transitViews, {
			val f = IntroExpandFragment.create()
			fragmentManager!!.beginTransaction()
					.replace(R.id.container, f)
					.addToBackStack(null)
					.commitAllowingStateLoss()
		}, resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
	}

	private fun enableInputs()
	{
		_next.setOnClickListener{onNextClick()}
	}

	private fun disableInputs()
	{
		_next.setOnClickListener(null)
	}

	private fun startAnimation()
	{
		if (!isViewDestroyed)
		{
			_touch.visibility = View.VISIBLE
			_clipContent.animate().translationX(120f)
					.setInterpolator(AccelerateDecelerateInterpolator())
					.setDuration(resources.getInteger(
							android.R.integer.config_longAnimTime).toLong())
		}
	}

	private val _handler = Handler()
	private val _next by lazyView<View>(R.id.next)
	private val _clipContent by lazyView<View>(R.id.clip_content)
	private val _touch by lazyView<View>(R.id.touch)
	private val _transitViews by viewAwareLazy{
		listOf(findView<View>(R.id.clip),
				findView(R.id.message))
	}
}

class IntroExpandFragment : IntroFragment()
{
	companion object
	{
		fun create(): IntroExpandFragment
		{
			return IntroExpandFragment()
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
			savedInstanceState: Bundle?): View
	{
		super.onCreateView(inflater, container, savedInstanceState)
		return inflater.inflate(R.layout.intro_expand_frag, container, false)
	}

	override fun onActivityCreated(savedInstanceState: Bundle?)
	{
		super.onActivityCreated(savedInstanceState)
		val callback = {
			if (!isViewDestroyed)
			{
				enableInputs()
				_handler.postDelayed(::startAnimation, 800)
			}
		}
		if (isRestoreFromBackstack)
		{
			startExitReverseTransition(_doneTransitViews, callback)
		}
		else
		{
			startEnterTransition(_normalTransitViews, callback)
		}
	}

	override fun onDestroyView()
	{
		super.onDestroyView()
		_handler.removeCallbacksAndMessages(null)
	}

	override fun onBackPressed(): Boolean
	{
		if (!super.onBackPressed())
		{
			startEnterReverseTransition(_normalTransitViews, {
				fragmentManager!!.popBackStack()
			}, 100L)
		}
		return true
	}

	private fun onNextClick()
	{
		disableInputs()
		startExitTransition(_doneTransitViews, {
			val f = IntroDoneFragment.create()
			fragmentManager!!.beginTransaction()
					.replace(R.id.container, f)
					.addToBackStack(null)
					.commitAllowingStateLoss()
		}, resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
	}

	private fun enableInputs()
	{
		_next.setOnClickListener{onNextClick()}
	}

	private fun disableInputs()
	{
		_next.setOnClickListener(null)
	}

	private fun startAnimation()
	{
		if (!isViewDestroyed)
		{
			_touch.visibility = View.VISIBLE
			_touch.alpha = 1f
			_touch.animate().alpha(0f)
					.setInterpolator(AccelerateInterpolator())
					.setDuration(resources.getInteger(
							android.R.integer.config_longAnimTime).toLong())
			_handler.postDelayed(::startAnimation, 3400)
		}
	}

	private val _handler = Handler()
	private val _next by lazyView<View>(R.id.next)
	private val _touch by lazyView<View>(R.id.touch)
	private val _normalTransitViews by viewAwareLazy{
		listOf(findView<View>(R.id.clip),
				findView(R.id.message))
	}
	private val _doneTransitViews by viewAwareLazy{
		listOf(findView(R.id.clip),
				findView(R.id.message),
				_next)
	}
}

class IntroDoneFragment : IntroFragment()
{
	companion object
	{
		fun create(): IntroDoneFragment
		{
			return IntroDoneFragment()
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
			savedInstanceState: Bundle?): View
	{
		super.onCreateView(inflater, container, savedInstanceState)
		return inflater.inflate(R.layout.intro_done_frag, container, false)
	}

	override fun onActivityCreated(savedInstanceState: Bundle?)
	{
		super.onActivityCreated(savedInstanceState)
		val callback = {
			if (!isViewDestroyed)
			{
				enableInputs()
			}
		}
		if (isRestoreFromBackstack)
		{
			startExitReverseTransition(_transitViews, callback)
		}
		else
		{
			startEnterTransition(_transitViews, callback)
		}
	}

	override fun onBackPressed(): Boolean
	{
		if (!super.onBackPressed())
		{
			startEnterReverseTransition(_transitViews, {
				fragmentManager!!.popBackStack()
			}, 100L)
		}
		return true
	}

	private fun onNextClick()
	{
		_pref.introLevel = 1
		_pref.commit()

		startActivity(Intent(context, ClipboardActivity::class.java))
		activity?.finish()
	}

	private fun enableInputs()
	{
		_next.setOnClickListener{onNextClick()}
	}

	private val _pref by lazy{Preference.from(context!!)}
	private val _next by lazyView<View>(R.id.next)
	private val _transitViews by viewAwareLazy{
		listOf(findView(R.id.message),
				_next)
	}
}
