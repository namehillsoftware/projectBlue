package com.lasthopesoftware.bluewater.tutorials

import com.namehillsoftware.handoff.promises.Promise

interface ManageTutorials {
	fun promiseIsTutorialShown(tutorialKey: String): Promise<Boolean>

	fun promiseTutorialMarked(tutorialKey: String): Promise<Unit>
}
