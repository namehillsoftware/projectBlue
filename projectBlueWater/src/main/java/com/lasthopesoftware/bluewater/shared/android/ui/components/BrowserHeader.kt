package com.lasthopesoftware.bluewater.shared.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import com.lasthopesoftware.bluewater.shared.android.ui.linearInterpolation
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions.viewPaddingUnit
import kotlin.math.pow

@Composable
fun rememberTitleStartPadding(progress: State<Float>) = remember {
	derivedStateOf {
		val acceleratedProgress = (1 - progress.value).pow(3).coerceIn(0f, 1f)
		val acceleratedInverseProgress = 1 - acceleratedProgress

		linearInterpolation(
			viewPaddingUnit,
			Dimensions.topMenuIconSizeWithPadding,
			acceleratedInverseProgress
		)
	}
}
