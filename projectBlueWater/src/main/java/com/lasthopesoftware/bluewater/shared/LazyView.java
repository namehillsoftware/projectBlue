package com.lasthopesoftware.bluewater.shared;

import android.app.Activity;
import android.support.annotation.IdRes;
import android.view.View;

import com.vedsoft.lazyj.Lazy;

import java.util.concurrent.Callable;

/**
 * Created by david on 6/18/16.
 */
public class LazyView<T extends View> extends Lazy<T> {

	public LazyView(Activity activity, @IdRes int viewId) {
		this(() -> (T) activity.findViewById(viewId));
	}

	public LazyView(View view, @IdRes int viewId) {
		this(() -> (T) view.findViewById(viewId));
	}

	public LazyView(Callable<T> initialization) {
		super(initialization);
	}
}
