package com.lasthopesoftware.bluewater

object ApplicationDependenciesContainer {

	private lateinit var attachedDependencies: ApplicationDependencies

	fun attach(applicationDependencies: ApplicationDependencies) {
		attachedDependencies = applicationDependencies
	}

	val applicationDependencies: ApplicationDependencies
		get() = attachedDependencies
}
