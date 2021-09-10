package com.lasthopesoftware.bluewater.tutorials

import com.namehillsoftware.handoff.promises.Promise

interface ManageTutorials {
	fun promiseWasTutorialShown(tutorialKey: String): Promise<Boolean>

	fun promiseTutorialMarked(tutorialKey: String): Promise<Unit>
}
