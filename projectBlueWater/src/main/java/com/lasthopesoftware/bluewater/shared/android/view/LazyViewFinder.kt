package com.lasthopesoftware.bluewater.shared.android.view

import android.app.Activity
import android.view.View
import androidx.annotation.IdRes
import com.namehillsoftware.lazyj.AbstractSynchronousLazy
import com.namehillsoftware.lazyj.CreateAndHold

class LazyViewFinder<TView : View> private constructor(private val lazyViewInitializer: CreateAndHold<TView>) {

	constructor(activity: Activity, @IdRes viewId: Int) : this(LazyActivityViewFinder<TView>(activity, viewId))
	constructor(view: View, @IdRes viewId: Int) : this(LazyViewBasedViewFinder<TView>(view, viewId))

	fun findView(): TView = lazyViewInitializer.getObject()

	private class LazyActivityViewFinder<T : View>(private val activity: Activity, @param:IdRes val viewId: Int) : AbstractSynchronousLazy<T>() {
		override fun create(): T = activity.findViewById(viewId) ?: throw MissingViewException(viewId)
	}

	private class LazyViewBasedViewFinder<T : View>(private val view: View, @param:IdRes val viewId: Int) : AbstractSynchronousLazy<T>() {
		override fun create(): T = view.findViewById(viewId) ?: throw MissingViewException(viewId)
	}
}
