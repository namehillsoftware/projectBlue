package com.lasthopesoftware.bluewater.shared;

import java.util.List;

import io.reactivex.ObservableSource;
import io.reactivex.Single;

/**
 * Created by david on 12/18/16.
 */

public interface IUsefulObservable<T> extends ObservableSource<T> {
	Single<List<T>> toList();
}
