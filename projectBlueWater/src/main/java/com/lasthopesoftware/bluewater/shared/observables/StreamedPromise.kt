package com.lasthopesoftware.bluewater.shared.observables

import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.Observable
import io.reactivex.functions.Function

fun <T, S : Iterable<T>> Promise<S>.stream(): Observable<T> = this.toMaybeObservable().flattenAsObservable(flatten())

@Suppress("UNCHECKED_CAST")
private fun <T> flatten(): Flattener<T> = singleFlattener as Flattener<T>

private val singleFlattener by lazy { Flattener<Any?>() }

private class Flattener<T> : Function<T, T> {
	override fun apply(t: T): T = t
}
