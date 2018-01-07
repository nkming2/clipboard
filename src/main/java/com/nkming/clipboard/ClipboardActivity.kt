package com.nkming.clipboard

import android.os.Bundle

class ClipboardActivity : BaseActivity()
{
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(R.layout.frag_activity)
		if (savedInstanceState == null)
		{
			supportFragmentManager.beginTransaction()
					.add(R.id.container, ClipboardFragment.create())
					.commit()
		}
	}
}
