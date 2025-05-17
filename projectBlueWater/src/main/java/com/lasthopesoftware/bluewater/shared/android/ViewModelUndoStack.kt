package com.lasthopesoftware.bluewater.shared.android

import androidx.lifecycle.ViewModel
import com.namehillsoftware.handoff.promises.Promise
import java.util.Stack

class ViewModelUndoStack : ViewModel(), UndoStack {
	private val backStack = Stack<() -> Promise<Boolean>>()

	override fun addAction(action: () -> Promise<Boolean>) {
		backStack.push(action)
	}

	override fun removeAction(action: () -> Promise<*>) {
		backStack.remove(action)
	}

	override fun pop(): (() -> Promise<Boolean>)? =
		if (backStack.isNotEmpty()) backStack.pop() else null
}
