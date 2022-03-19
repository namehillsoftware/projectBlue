package com.lasthopesoftware.bluewater.about

import android.content.Context
import com.lasthopesoftware.bluewater.R

class AboutTitleBuilder(context: Context) : BuildAboutTitle {
	private val lazyAboutTitle by lazy {
		context
			.getString(R.string.title_activity_about)
			.format(context.getString(R.string.app_name))
	}

	override fun buildTitle(): String = lazyAboutTitle
}
