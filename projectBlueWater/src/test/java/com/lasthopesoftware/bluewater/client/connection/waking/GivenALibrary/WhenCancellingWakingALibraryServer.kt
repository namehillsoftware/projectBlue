package com.lasthopesoftware.bluewater.client.connection.waking.GivenALibrary

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.waking.AlarmConfiguration
import com.lasthopesoftware.bluewater.client.connection.waking.MachineAddress
import com.lasthopesoftware.bluewater.client.connection.waking.PokeServer
import com.lasthopesoftware.bluewater.client.connection.waking.ServerAlarm
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class WhenCancellingWakingALibraryServer {

	private val expectedCancelledPokes = arrayOf(
		MachineAddress("local-address", "AB-E0-9F-24-F5"),
		MachineAddress("local-address", "99-53-7F-2C-A1"),
		MachineAddress("second-local-address", "AB-E0-9F-24-F5"),
		MachineAddress("second-local-address", "99-53-7F-2C-A1"),
		MachineAddress("remote-address", "AB-E0-9F-24-F5"),
		MachineAddress("remote-address", "99-53-7F-2C-A1")
	)

	private val mut by lazy {
		val lookupServers = mockk<LookupServers>().apply {
			every { promiseServerInformation(any()) } returns Promise<ServerInfo?>(
				ServerInfo(
					5001,
					5002,
					"remote-address",
					listOf("local-address", "second-local-address"),
					listOf("AB-E0-9F-24-F5", "99-53-7F-2C-A1"),
					null
				)
			)
		}

		val pokeServer = mockk<PokeServer>().apply {
			every { promiseWakeSignal(any(), any(), any()) } returns Unit.toPromise()

			every { promiseWakeSignal(any(), 4, Duration.standardSeconds(60)) } answers {
				Promise { m ->
					m.awaitCancellation {
						cancelledPokes.add(firstArg())
						m.sendResolution(Unit)
					}
				}
			}
		}

		val serverAlarm = ServerAlarm(
			lookupServers,
			pokeServer,
			AlarmConfiguration(4, Duration.standardMinutes(1))
		)

		serverAlarm
	}

	private val cancelledPokes: MutableList<MachineAddress> = ArrayList()

	@BeforeAll
	fun act() {
		val promisedLibraryServerPoke = mut.awakeLibraryServer(LibraryId(14))
		promisedLibraryServerPoke.cancel()
		promisedLibraryServerPoke.toExpiringFuture()[5, TimeUnit.SECONDS]
	}

	@Test
	fun `then the pokes are cancelled`() {
		assertThat(cancelledPokes).containsExactlyInAnyOrder(*expectedCancelledPokes)
	}
}
