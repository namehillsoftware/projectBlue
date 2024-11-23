package com.lasthopesoftware.observables

import com.lasthopesoftware.bluewater.shared.NullBox
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable

fun Disposable.toCloseable() = AutoCloseable { dispose() }

fun <T> InteractionState<T>.asInteractionState() = this

fun <T> Observable<NullBox<T>>.filterNonNull() = filter { it.value != null }.map { it.value!! }
