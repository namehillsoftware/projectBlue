package com.lasthopesoftware.bluewater.shared.android

import androidx.lifecycle.ViewModel
import com.namehillsoftware.handoff.promises.Promise
import java.util.Stack

class ViewModelUndoStack : ViewModel(), UndoStack {
	private val backStack = Stack<() -> Promise<Boolean>>()

	override fun addAction(action: () -> Promise<Boolean>): AutoCloseable {
		backStack.push(action)
		return CloseableAction(backStack, action)
	}

	override fun removeAction(action: () -> Promise<*>) {
		backStack.remove(action)
	}

	override fun pop(): (() -> Promise<Boolean>)? =
		if (backStack.isNotEmpty()) backStack.pop() else null

	private class CloseableAction(
		private val stack: Stack<() -> Promise<Boolean>>,
		private val action: () -> Promise<Boolean>) : AutoCloseable {
		override fun close() {
			stack.remove(action)
		}
	}
}
