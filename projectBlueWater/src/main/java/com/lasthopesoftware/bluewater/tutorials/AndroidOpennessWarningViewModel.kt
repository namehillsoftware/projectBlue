package com.lasthopesoftware.bluewater.tutorials

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.observables.MutableInteractionState
import com.namehillsoftware.handoff.promises.Promise

class AndroidOpennessWarningViewModel(
	private val tutorials: ManageTutorials
) : ViewModel(), TrackLoadedViewState {

	private val mutableIsLoading = MutableInteractionState(false)
	private val mutableIsShown = MutableInteractionState(false)

	val isShown = mutableIsShown.asInteractionState()

	override val isLoading = mutableIsLoading.asInteractionState()

	fun promiseLoadedSideLoadingTutorial(): Promise<Unit> {
		mutableIsLoading.value = true
		return tutorials
			.promiseWasTutorialShown(TutorialManager.KnownTutorials.droidSideLoadingWarning)
			.then { mutableIsShown.value = !it }
			.must { _ -> mutableIsLoading.value = false  }
	}

	fun promiseSideLoadingMarkedShown(): Promise<Unit> {
		mutableIsShown.value = false
		return tutorials.promiseTutorialMarked(TutorialManager.KnownTutorials.droidSideLoadingWarning)
	}
}
