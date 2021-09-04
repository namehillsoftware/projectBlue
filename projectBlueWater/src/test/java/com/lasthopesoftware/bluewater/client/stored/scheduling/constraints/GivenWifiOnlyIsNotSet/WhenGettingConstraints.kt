package com.lasthopesoftware.bluewater.client.stored.scheduling.constraints.GivenWifiOnlyIsNotSet

import androidx.work.Constraints
import androidx.work.NetworkType
import com.lasthopesoftware.bluewater.client.stored.scheduling.constraints.SyncWorkerConstraints
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.BeforeClass
import org.junit.Test

class WhenGettingConstraints {

	companion object {
		private var constraints: Constraints? = null

		@JvmStatic
		@BeforeClass
		fun before() {
			val applicationSettings = mockk<HoldApplicationSettings>()
			every { applicationSettings.promiseApplicationSettings() } returns Promise(ApplicationSettings())
			val syncWorkerConstraints = SyncWorkerConstraints(applicationSettings)
			constraints = syncWorkerConstraints.currentConstraints.toFuture().get()
		}
	}

    @Test
    fun thenTheConstraintsAreCorrect() {
        assertThat(constraints!!.requiredNetworkType).isEqualTo(NetworkType.CONNECTED)
    }
}
