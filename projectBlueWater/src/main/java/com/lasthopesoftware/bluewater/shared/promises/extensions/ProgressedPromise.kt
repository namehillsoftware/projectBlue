package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise

abstract class ProgressedPromise<Progress, Resolution> : Promise<Resolution> {

	constructor(resolution: Resolution?) : super(resolution)

	constructor(rejection: Throwable) : super(rejection)

	constructor(messengerOperator: MessengerOperator<Resolution>?) : super(messengerOperator)
	protected constructor()

	abstract val progress: Promise<Progress>
}
