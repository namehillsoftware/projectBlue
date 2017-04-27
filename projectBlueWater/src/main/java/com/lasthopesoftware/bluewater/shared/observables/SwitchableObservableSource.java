package com.lasthopesoftware.bluewater.shared.observables;


import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;

public class SwitchableObservableSource<T> implements ObservableOnSubscribe<T> {

	private Disposable subscription;
	private Observable<T> observable;
	private Subscriber<T> subscriber;

	public SwitchableObservableSource(Observable<T> source) {
		observable = source;
	}

	@Override
	public void subscribe(@NonNull ObservableEmitter<T> e) throws Exception {
		if (subscription != null)
			subscription.dispose();

		subscription = observable.subscribeWith(new Subscriber<>(e)).disposable;
	}

	public void switchSource(Observable<T> newSource) {

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
