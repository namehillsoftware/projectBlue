package com.lasthopesoftware.bluewater.client.stored.sync.constraints

import androidx.work.Constraints
import com.namehillsoftware.handoff.promises.Promise

interface ConstrainSyncWork {
    val currentConstraints: Promise<Constraints>
}
