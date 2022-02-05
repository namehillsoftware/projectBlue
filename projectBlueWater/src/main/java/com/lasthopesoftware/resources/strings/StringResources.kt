package com.lasthopesoftware.resources.strings

import android.content.Context
import com.lasthopesoftware.bluewater.R

class StringResources(context: Context): GetStringResources {
	override val loading by lazy { context.getString(R.string.lbl_loading) }
}
