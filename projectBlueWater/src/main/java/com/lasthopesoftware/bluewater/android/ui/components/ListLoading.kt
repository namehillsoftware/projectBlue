package com.lasthopesoftware.bluewater.android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier


@Composable
fun ListLoading(modifier: Modifier) {
	Box(modifier = modifier) {
		CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
	}
}
