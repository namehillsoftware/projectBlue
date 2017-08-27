package com.lasthopesoftware.bluewater.shared;

import java.util.List;

import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * Created by david on 12/18/16.
 */

public interface IUsefulObservable<T> extends ObservableSource<T> {
	Single<List<T>> toList();
	Disposable subscribe(Consumer<? super T> onNext);
}
