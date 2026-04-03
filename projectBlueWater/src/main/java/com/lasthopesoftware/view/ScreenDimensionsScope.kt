package com.lasthopesoftware.view

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.lasthopesoftware.bluewater.android.ui.remember

class ScreenDimensionsScope(val screenHeight: Dp, val screenWidth: Dp, innerBoxScope: BoxWithConstraintsScope) :
	BoxWithConstraintsScope by innerBoxScope

@Composable
fun BoxWithConstraintsScope.isNarrow() = LocalDensity.current.remember { maxWidth < maxHeight }
