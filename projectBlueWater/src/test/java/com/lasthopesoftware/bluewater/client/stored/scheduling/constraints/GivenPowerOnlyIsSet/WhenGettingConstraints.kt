package com.lasthopesoftware.bluewater.client.stored.scheduling.constraints.GivenPowerOnlyIsSet

import com.lasthopesoftware.bluewater.client.stored.sync.constraints.SyncWorkerConstraints
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test

class WhenGettingConstraints {

	private val constraints by lazy {
		val applicationSettings = mockk<HoldApplicationSettings>()
		every { applicationSettings.promiseApplicationSettings() } returns Promise(ApplicationSettings(isSyncOnPowerOnly = true))
		val syncWorkerConstraints = SyncWorkerConstraints(applicationSettings)
		syncWorkerConstraints.currentConstraints.toExpiringFuture().get()
	}

	@Test
	fun `then the constraints are correct`() {
		assertThat(constraints!!.requiresCharging()).isTrue
	}
}
