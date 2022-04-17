package com.lasthopesoftware.bluewater.client.connection.waking.GivenAMachine

import com.lasthopesoftware.bluewater.client.connection.SendPackets
import com.lasthopesoftware.bluewater.client.connection.waking.MachineAddress
import com.lasthopesoftware.bluewater.client.connection.waking.ServerWakeSignal
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test

class WhenSendingAWakeSignal {

	companion object {
		private val expectedBytes = byteArrayOf(-1, -1, -1, -1, -1, -1, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37)
		private var sentBytes: ByteArray? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val sendPackets = mockk<SendPackets>()
				every { sendPackets.promiseSentPackets("http://my-sleeping-beauty", 9, any()) } answers {
					sentBytes = lastArg()
					Unit.toPromise()
				}
			ServerWakeSignal(sendPackets).promiseWakeSignal(
					MachineAddress(
						"http://my-sleeping-beauty",
						"01-58-87-FA-91-25"
					),
					1,
					Duration.ZERO
				).toExpiringFuture().get()
		}
	}

	@Test
	fun thenTheSignalIsCorrect() {
		assertThat(sentBytes).containsExactly(*expectedBytes)
	}
}
