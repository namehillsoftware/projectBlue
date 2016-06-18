package com.lasthopesoftware.bluewater.shared;

import android.app.Activity;
import android.support.annotation.IdRes;
import android.view.View;

import com.vedsoft.lazyj.AbstractLazy;

/**
 * Created by david on 6/18/16.
 */
public class LazyView<T extends View> extends AbstractLazy<T> {

	private final int viewId;
	private final Activity activity;

	public LazyView(Activity activity, @IdRes int viewId) {
		this.activity = activity;
		this.viewId = viewId;
	}

	@Override
	protected T initialize() throws Exception {
		return (T) activity.findViewById(viewId);
	}
}
