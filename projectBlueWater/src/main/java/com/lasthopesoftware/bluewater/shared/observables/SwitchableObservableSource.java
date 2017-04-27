package com.lasthopesoftware.bluewater.shared.observables;


import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;

public class SwitchableObservableSource<T> implements ObservableOnSubscribe<T>, AutoCloseable {

	private volatile Disposable subscription;
	private volatile Observable<T> observable;
	private volatile Subscriber<T> subscriber;

	public SwitchableObservableSource(Observable<T> source) {
		observable = source;
	}

	@Override
	public void subscribe(@NonNull ObservableEmitter<T> e) throws Exception {
		subscriber = new Subscriber<>(e);
		if (subscriber != null)
			updateSubscription(subscriber);
	}

	public void switchSource(Observable<T> newSource) {
		observable = newSource;

		if (subscriber != null)
			updateSubscription(subscriber);
	}

	private void updateSubscription(Subscriber<T> subscriber) {
		final Disposable newSubscription = observable.subscribeWith(subscriber).disposable;
		if (subscription != null)
			subscription.dispose();

		subscription = newSubscription;
	}

	@Override
	public void close() {
		if (subscription != null)
			subscription.dispose();
	}

	private static class Subscriber<T> implements Observer<T> {

		private final ObservableEmitter<T> emitter;
		private Disposable disposable;

		Subscriber(ObservableEmitter<T> sourceEmitter) {
			emitter = sourceEmitter;
		}

		@Override
		public void onSubscribe(Disposable d) {
			disposable = d;
		}

		@Override
		public void onNext(T t) {
			if (emitter != null)
				emitter.onNext(t);
		}

		@Override
		public void onError(Throwable e) {
			if (emitter != null)
				emitter.onError(e);
		}

		@Override
		public void onComplete() {
			if (emitter != null)
				emitter.onComplete();
		}
	}
}
