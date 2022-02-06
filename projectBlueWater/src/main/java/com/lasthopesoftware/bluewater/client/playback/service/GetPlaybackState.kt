package com.lasthopesoftware.bluewater.client.playback.service

import com.namehillsoftware.handoff.promises.Promise

interface GetPlaybackState {
	fun promiseIsMarkedForPlay(): Promise<Boolean>
}
