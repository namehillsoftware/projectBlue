package com.lasthopesoftware.bluewater.client.connection.waking.GivenAMachine

import com.annimon.stream.Stream
import com.lasthopesoftware.bluewater.client.connection.SendPackets
import com.lasthopesoftware.bluewater.client.connection.waking.MachineAddress
import com.lasthopesoftware.bluewater.client.connection.waking.ServerWakeSignal
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test
import java.util.*

class WhenSendingAWakeSignalTwiceWithFiveSecondsBetween {
	@Test
	fun thenTheSignalIsCorrect() {
		val allExpectedBytes =
			Stream.rangeClosed(1, 4).flatMap { i: Int? -> Stream.of(*expectedBytes) }
				.toList()
		Assertions.assertThat(sentBytes).containsExactlyElementsOf(allExpectedBytes)
	}

	companion object {
		private val expectedBytes = arrayOf<Byte>(
			-1,
			-1,
			-1,
			-1,
			-1,
			-1,
			1,
			88,
			-121,
			-6,
			-111,
			37,
			1,
			88,
			-121,
			-6,
			-111,
			37,
			1,
			88,
			-121,
			-6,
			-111,
			37,
			1,
			88,
			-121,
			-6,
			-111,
			37,
			1,
			88,
			-121,
			-6,
			-111,
			37,
			1,
			88,
			-121,
			-6,
			-111,
			37,
			1,
			88,
			-121,
			-6,
			-111,
			37,
			1,
			88,
			-121,
			-6,
			-111,
			37,
			1,
			88,
			-121,
			-6,
			-111,
			37,
			1,
			88,
			-121,
			-6,
			-111,
			37,
			1,
			88,
			-121,
			-6,
			-111,
			37,
			1,
			88,
			-121,
			-6,
			-111,
			37,
			1,
			88,
			-121,
			-6,
			-111,
			37,
			1,
			88,
			-121,
			-6,
			-111,
			37,
			1,
			88,
			-121,
			-6,
			-111,
			37,
			1,
			88,
			-121,
			-6,
			-111,
			37
		)
		private val sentBytes: MutableList<Byte> = ArrayList()

		@BeforeClass
		@JvmStatic
		fun before() {
			val connectionProvider = mockk<SendPackets>()
			every { connectionProvider.promiseSentPackets("http://my-sleeping-beauty", 9, any()) } answers {
				sentBytes.addAll(lastArg<ByteArray>().toList())
				Unit.toPromise()
			}
			val serverWakeSignal = ServerWakeSignal(connectionProvider)
			FuturePromise(
				serverWakeSignal.promiseWakeSignal(
					MachineAddress(
						"http://my-sleeping-beauty",
						"01-58-87-FA-91-25"
					),
					4,
					Duration.standardSeconds(2)
				)
			).get()
		}
	}
}
