package com.lasthopesoftware.bluewater.tutorials

interface CacheTutorialInformation {
	operator fun set(tutorialKey: String, wasShown: Boolean)
	operator fun get(tutorialKey: String): Boolean?
}
