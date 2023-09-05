package com.lasthopesoftware.bluewater.shared.observables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Composable
fun <T, S : ReadOnlyStateObservable<T>> S.subscribeAsState(): State<T> {
	val state = remember { mutableStateOf(value) }
	DisposableEffect(this) {
		val disposable = subscribe {
			state.value = it
		}
		onDispose { disposable.dispose() }
	}
	return state
}

@Composable
fun <T, S : MutableStateObservable<T>> S.subscribeAsMutableState(
	context: CoroutineContext = EmptyCoroutineContext
): MutableState<T> {
	val state = remember { mutableStateOf(value) }
	DisposableEffect(key1 = this) {
		val disposable = subscribe { state.value = it }

		onDispose {
			disposable.dispose()
		}
	}

	LaunchedEffect(key1 = this) {
		value = state.value
		if (context == EmptyCoroutineContext) {
			snapshotFlow { state.value }.collect {
				value = it
			}
		} else withContext(context) {
			snapshotFlow { state.value }.collect {
				value = it
			}
		}
	}
	return state
}
