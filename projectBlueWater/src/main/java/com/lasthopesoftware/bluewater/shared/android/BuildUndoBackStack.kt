package com.lasthopesoftware.bluewater.shared.android

import com.namehillsoftware.handoff.promises.Promise

interface BuildUndoBackStack {
	fun addAction(action: () -> Promise<Boolean>)
	fun removeAction(action: () -> Promise<*>)
}
