package com.lasthopesoftware.bluewater.settings.volumeleveling

import com.namehillsoftware.handoff.promises.Promise

interface IVolumeLevelSettings {
    val isVolumeLevellingEnabled: Promise<Boolean>
}
