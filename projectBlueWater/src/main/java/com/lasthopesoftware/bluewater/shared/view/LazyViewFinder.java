package com.lasthopesoftware.bluewater.shared.view;

import android.app.Activity;
import android.support.annotation.IdRes;
import android.view.View;

import com.vedsoft.lazyj.AbstractThreadLocalLazy;
import com.vedsoft.lazyj.ILazy;

/**
 * Created by david on 6/18/16.
 */
public class LazyViewFinder<TView extends View> {

	private final ILazy<TView> lazyViewInitializer;

	public LazyViewFinder(Activity activity, @IdRes int viewId) {
		this(new LazyActivityViewFinder<>(activity, viewId));
	}

	public LazyViewFinder(View view, @IdRes int viewId) {
		this(new LazyViewBasedViewFinder<>(view, viewId));
	}

	private LazyViewFinder(ILazy<TView> lazyViewInitializer) {
		this.lazyViewInitializer = lazyViewInitializer;
	}

	public TView findView() {
		return lazyViewInitializer.getObject();
	}

	private static final class LazyActivityViewFinder<T extends View> extends AbstractThreadLocalLazy<T> {

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

	private static final class LazyViewBasedViewFinder<T extends View> extends AbstractThreadLocalLazy<T> {

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
