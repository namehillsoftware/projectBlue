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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenSendingAWakeSignalTwiceWithFiveSecondsBetween {

	private val expectedBytes = listOf<Byte>(-1, -1, -1, -1, -1, -1, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37,1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37)
	private val sentBytes = ArrayList<Byte>()

	@BeforeAll
	fun act() {
		val connectionProvider = mockk<SendPackets>().apply {
			every { promiseSentPackets("http://my-sleeping-beauty", 9, any()) } answers {
				sentBytes.addAll(lastArg<ByteArray>().toList())
				Unit.toPromise()
			}
		}
		ServerWakeSignal(connectionProvider)
			.promiseWakeSignal(
				MachineAddress(
					"http://my-sleeping-beauty",
					"01-58-87-FA-91-25"
				),
				4,
				Duration.standardSeconds(2)
			)
			.toExpiringFuture()
			.get()
	}

	@Test
	fun `then the signal is correct`() {
		val allExpectedBytes = (1..4).flatMap { expectedBytes }
		assertThat(sentBytes).containsExactlyElementsOf(allExpectedBytes)
	}
}
