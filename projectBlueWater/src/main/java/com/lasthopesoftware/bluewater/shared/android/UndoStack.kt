package com.lasthopesoftware.bluewater.shared.android

import com.namehillsoftware.handoff.promises.Promise

interface UndoStack {
	fun addAction(action: () -> Promise<Boolean>)
	fun removeAction(action: () -> Promise<*>)
	fun pop(): (() -> Promise<Boolean>)?
}
