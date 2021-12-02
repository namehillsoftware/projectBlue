package com.lasthopesoftware.bluewater.shared.android.MediaSession

import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder

object MediaSessionConstants {
	private val magicPropertyBuilder by lazy { MagicPropertyBuilder(MediaSessionConstants::class.java) }

	val mediaSessionTag by lazy { magicPropertyBuilder.buildProperty("mediaSessionTag") }
}
