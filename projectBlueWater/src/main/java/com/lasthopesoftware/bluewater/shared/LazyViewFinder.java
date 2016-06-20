package com.lasthopesoftware.bluewater.shared;

import android.app.Activity;
import android.support.annotation.IdRes;
import android.view.View;

import com.vedsoft.lazyj.AbstractLazy;

/**
 * Created by david on 6/18/16.
 */
public class LazyViewFinder<T extends View> {

	private final AbstractLazy<T> lazyViewInitializer;

	public LazyViewFinder(Activity activity, @IdRes int viewId) {
		this(new LazyActivityViewFinder<>(activity, viewId));
	}

	public LazyViewFinder(View view, @IdRes int viewId) {
		this(new LazyViewBasedViewFinder<>(view, viewId));
	}

	private LazyViewFinder(AbstractLazy<T> lazyViewInitializer) {
		this.lazyViewInitializer = lazyViewInitializer;
	}

	public T findView() {
		return lazyViewInitializer.getObject();
	}

	private static class LazyActivityViewFinder<T extends View> extends AbstractLazy<T> {

		private final Activity activity;
		private final int viewId;

		public LazyActivityViewFinder(Activity activity, @IdRes int viewId) {
			this.activity = activity;
			this.viewId = viewId;
		}

		@Override
		protected T initialize() throws Exception {
			return (T) activity.findViewById(viewId);
		}
	}

	private static class LazyViewBasedViewFinder<T extends View> extends AbstractLazy<T> {

		private final View view;
		private final int viewId;

		public LazyViewBasedViewFinder(View view, @IdRes int viewId) {
			this.view = view;
			this.viewId = viewId;
		}

		@Override
		protected T initialize() throws Exception {
			return (T) view.findViewById(viewId);
		}
	}
}
