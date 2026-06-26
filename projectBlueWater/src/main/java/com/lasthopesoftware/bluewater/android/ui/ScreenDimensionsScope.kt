package com.lasthopesoftware.bluewater.android.ui

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.ui.unit.Dp

class ScreenDimensionsScope(val screenHeight: Dp, val screenWidth: Dp, innerBoxScope: BoxWithConstraintsScope) :
	BoxWithConstraintsScope by innerBoxScope

val BoxWithConstraintsScope.isNarrow
	get() = maxWidth < maxHeight
