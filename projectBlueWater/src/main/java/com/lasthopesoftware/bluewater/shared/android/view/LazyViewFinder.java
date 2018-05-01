package com.lasthopesoftware.bluewater.shared.android.view;

import android.app.Activity;
import android.support.annotation.IdRes;
import android.view.View;

import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

public class LazyViewFinder<TView extends View> {

	private final CreateAndHold<TView> lazyViewInitializer;

	public LazyViewFinder(Activity activity, @IdRes int viewId) {
		this(new LazyActivityViewFinder<>(activity, viewId));
	}

	public LazyViewFinder(View view, @IdRes int viewId) {
		this(new LazyViewBasedViewFinder<>(view, viewId));
	}

	private LazyViewFinder(CreateAndHold<TView> lazyViewInitializer) {
		this.lazyViewInitializer = lazyViewInitializer;
	}

	public TView findView() {
		return lazyViewInitializer.getObject();
	}

	private static final class LazyActivityViewFinder<T extends View> extends AbstractSynchronousLazy<T> {

		private final Activity activity;
		private final int viewId;

		LazyActivityViewFinder(Activity activity, @IdRes int viewId) {
			this.activity = activity;
			this.viewId = viewId;
		}

		@Override
		protected T create() {
			return activity.findViewById(viewId);
		}
	}

	private static final class LazyViewBasedViewFinder<T extends View> extends AbstractSynchronousLazy<T> {

		private final View view;
		private final int viewId;

		LazyViewBasedViewFinder(View view, @IdRes int viewId) {
			this.view = view;
			this.viewId = viewId;
		}

		@Override
		protected T create() {
			return view.findViewById(viewId);
		}
	}
}
