package com.lasthopesoftware.bluewater.shared.android.ui

import android.content.Context

class ScreenDimensions(private val context: Context) : ProvideScreenDimensions {
	private val displayMetrics by lazy {
		context.resources.displayMetrics
	}

	override val heightPixels: Int
		get() = displayMetrics.heightPixels

	override val widthPixels: Int
		get() = displayMetrics.widthPixels
}
