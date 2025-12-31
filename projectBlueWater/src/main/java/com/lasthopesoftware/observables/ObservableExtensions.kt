package com.lasthopesoftware.observables

import com.lasthopesoftware.bluewater.shared.NullBox
import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.MaybeObserver
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleObserver
import io.reactivex.rxjava3.disposables.Disposable

fun Disposable.toCloseable() = AutoCloseable { dispose() }

fun <T> InteractionState<T>.asInteractionState() = this

fun <T> Observable<NullBox<T>>.mapNotNull() = filter { it.value != null }.map { it.value!! }

fun <T : Any> Promise<T?>.toMaybeObservable(): Maybe<T> = MaybeObservePromise(this)

private class MaybeObservePromise<T : Any>(private val promise: Promise<T?>) :
	Maybe<T>(),
	Disposable
{
	@Volatile
	private var isCancelled = false

	override fun subscribeActual(observer: MaybeObserver<in T>) {
		observer.onSubscribe(this)
		promise.then(
			{ t -> if (t != null) observer.onSuccess(t) else observer.onComplete() },
			observer::onError)
	}

	override fun dispose() {
		isCancelled = true
		promise.cancel()
	}

	override fun isDisposed(): Boolean = isCancelled
}

fun <T : Any> Promise<T>.toSingleObservable(): Single<T> = SingleObservePromise(this)

private class SingleObservePromise<T : Any>(private val promise: Promise<T>) :
	Single<T>(),
	Disposable
{
	@Volatile
	private var isCancelled = false

	override fun subscribeActual(observer: SingleObserver<in T>) {
		observer.onSubscribe(this)
		promise.then(
			observer::onSuccess,
			observer::onError)
	}

	override fun dispose() {
		isCancelled = true
		promise.cancel()
	}

	override fun isDisposed(): Boolean = isCancelled
}
