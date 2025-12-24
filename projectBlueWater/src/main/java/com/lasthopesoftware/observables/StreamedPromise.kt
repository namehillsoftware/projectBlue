package com.lasthopesoftware.observables

import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.functions.Function

fun <T : Any, S : Iterable<T>> Promise<S>.stream(): Observable<T> = this.toMaybeObservable().flattenAsObservable(flatten())

@Suppress("UNCHECKED_CAST")
private fun <T : Any> flatten(): Flattener<T> = singleFlattener as Flattener<T>

private val singleFlattener by lazy { Flattener<Any>() }

private class Flattener<T : Any> : Function<T, T> {
	override fun apply(t: T): T = t
}
