package com.lasthopesoftware.bluewater.tutorials

import java.util.concurrent.ConcurrentHashMap

object TutorialInformationCache : CacheTutorialInformation {
	private val tutorialManagerCache = ConcurrentHashMap<String, Boolean>()

	override fun set(tutorialKey: String, wasShown: Boolean) {
		tutorialManagerCache[tutorialKey] = wasShown
	}

	override fun get(tutorialKey: String): Boolean? = tutorialManagerCache[tutorialKey]
}
