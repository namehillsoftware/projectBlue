package com.lasthopesoftware.bluewater.shared.android

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.Stack

class UndoStackApplicationNavigation(
	private val inner: NavigateApplication,
	onBackPressedDispatcher: OnBackPressedDispatcher,
) : ViewModel(), NavigateApplication by inner, BuildUndoBackStack {
	private val onBackPressedCallback = object : OnBackPressedCallback(true) {
		override fun handleOnBackPressed() {
			backOut()
		}
	}

	private val backStack = Stack<() -> Promise<Boolean>>()

	init {
	    onBackPressedDispatcher.addCallback(onBackPressedCallback)
	}

	override fun onCleared() {
		onBackPressedCallback.remove()
	}

	override fun addAction(action: () -> Promise<Boolean>) {
		backStack.push(action)
	}

	override fun removeAction(action: () -> Promise<*>) {
		backStack.remove(action)
	}

	override fun backOut(): Promise<Boolean> =
		if (backStack.isNotEmpty()) backStack.pop()().eventually { if (it) it.toPromise() else backOut() }
		else inner.backOut()
}
