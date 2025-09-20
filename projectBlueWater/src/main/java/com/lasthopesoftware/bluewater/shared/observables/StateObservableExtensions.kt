package com.lasthopesoftware.bluewater.shared.observables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Composable
fun <T, S : InteractionState<T>> S.subscribeAsState(
	context: CoroutineContext = EmptyCoroutineContext
): State<T> = updatingState(context)

@Composable
fun <T, S : MutableInteractionState<T>> S.subscribeAsMutableState(
	context: CoroutineContext = EmptyCoroutineContext
): MutableState<T> {
	val state = updatingState(context)

	LaunchedEffect(key1 = this, context) {
		if (context == EmptyCoroutineContext) {
			snapshotFlow { state.value }.drop(1).collect {
				value = it
			}
		} else withContext(context) {
			snapshotFlow { state.value }.drop(1).collect {
				value = it
			}
		}
	}
	return state
}

@Composable
private fun <T, S : InteractionState<T>> S.updatingState(context: CoroutineContext): MutableState<T> {
	val state = remember { mutableStateOf(value) }
	LaunchedEffect(this, context) {
		if (context == EmptyCoroutineContext) {
			asFlow().collect {
				state.value = it.value
			}
		} else withContext(context) {
			asFlow().collect {
				state.value = it.value
			}
		}
	}
	return state
}
