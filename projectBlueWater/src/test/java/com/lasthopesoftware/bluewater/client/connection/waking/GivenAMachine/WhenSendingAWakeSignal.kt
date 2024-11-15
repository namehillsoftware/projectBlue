package com.lasthopesoftware.bluewater.client.connection.waking.GivenAMachine

import com.lasthopesoftware.bluewater.client.connection.SendPackets
import com.lasthopesoftware.bluewater.client.connection.waking.MachineAddress
import com.lasthopesoftware.bluewater.client.connection.waking.ServerWakeSignal
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenSendingAWakeSignal {

	private val expectedBytes = byteArrayOf(-1, -1, -1, -1, -1, -1, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37)
	private var sentBytes: ByteArray? = null

	@BeforeAll
	fun act() {
		val sendPackets = mockk<SendPackets>()
			every { sendPackets.promiseSentPackets("http://my-sleeping-beauty", 9, any()) } answers {
				sentBytes = lastArg()
				Unit.toPromise()
			}
		ServerWakeSignal(sendPackets)
			.promiseWakeSignal(
				MachineAddress(
					"http://my-sleeping-beauty",
					"01-58-87-FA-91-25"
				)
			)
			.toExpiringFuture()
			.get()
	}

	@Test
	fun `then the signal is correct`() {
		assertThat(sentBytes).containsExactly(*expectedBytes)
	}
}
