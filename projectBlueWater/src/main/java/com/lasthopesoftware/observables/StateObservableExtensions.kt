package com.lasthopesoftware.observables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Composable
fun <T, S : InteractionState<T>> S.subscribeAsState(): State<T> = updatingState()

@Composable
fun <T, S : MutableInteractionState<T>> S.subscribeAsMutableState(
	context: CoroutineContext = EmptyCoroutineContext
): MutableState<T> {
	val state = updatingState()

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
inline fun <T, S : InteractionState<T>, R : Any> S.subscribeAsState(
	crossinline transform: (T) -> R,
	noinline onEach: (S.() -> Observable<R>)? = null,
): State<R> {
	val onEach = onEach ?: { mapNotNull().map { transform(it) } }
	val state = remember { mutableStateOf(transform(value)) }
	DisposableEffect(this) {
		val disposable = onEach().subscribe()
		onDispose { disposable.dispose() }
	}
	return state
}

@Composable
inline fun <T : Any, S : InteractionState<T>> S.subscribeAsState(
	crossinline onEach: S.() -> Observable<T>,
): State<T> {
	val state = remember { mutableStateOf(value) }
	DisposableEffect(this) {
		val disposable = onEach().subscribe()
		onDispose { disposable.dispose() }
	}
	return state
}

@Composable
private fun <T, S : InteractionState<T>> S.updatingState(): MutableState<T> {
	val state = remember { mutableStateOf(value) }
	DisposableEffect(this) {
		val subscription = subscribe {
			state.value = it.value
		}

		onDispose {
			subscription.dispose()
		}
	}
	return state
}
