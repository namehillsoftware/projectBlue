package com.lasthopesoftware.bluewater.shared.android.viewmodels

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Composable
fun <T> StateFlow<T>.collectAsMutableState(
	context: CoroutineContext = EmptyCoroutineContext
): MutableState<T> {
	val mutableState = remember(this) {
		mutableStateOf(value)
	}

	LaunchedEffect(key1 = this) {
		if (context == EmptyCoroutineContext) {
			collect { mutableState.value = it }
		} else withContext(context) {
			collect { mutableState.value = it }
		}
	}

	return mutableState
}
