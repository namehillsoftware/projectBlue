package com.lasthopesoftware.bluewater.shared.android

import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class UndoStackApplicationNavigation(
	private val undoStack: UndoStack,
	private val inner: NavigateApplication,
) : NavigateApplication by inner {
	override fun backOut(): Promise<Boolean> =
		undoStack
			.pop()
			?.invoke()
			?.eventually { if (it) it.toPromise() else backOut() }
			?: inner.backOut()
}
