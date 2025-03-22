package com.lasthopesoftware.bluewater.client.connection.waking.GivenALibrary

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.waking.MachineAddress
import com.lasthopesoftware.bluewater.client.connection.waking.PokeServer
import com.lasthopesoftware.bluewater.client.connection.waking.ServerAlarm
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
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
		MachineAddress("remote-address", "99-53-7F-2C-A1"),
		MachineAddress("255.255.255.255", "99-53-7F-2C-A1"),
		MachineAddress("255.255.255.255", "AB-E0-9F-24-F5"),
	)

	private val mut by lazy {
		val lookupServers = mockk<LookupServers>().apply {
			every { promiseServerInformation(any()) } returns Promise(
				ServerInfo(
					5001,
					5002,
					"remote-address",
					setOf("local-address", "second-local-address"),
					setOf("AB-E0-9F-24-F5", "99-53-7F-2C-A1"),
					ByteArray(0)
				)
			)
		}

		val pokeServer = mockk<PokeServer>().apply {
			every { promiseWakeSignal(any()) } returns Unit.toPromise()

			every { promiseWakeSignal(any()) } answers {
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
			mockk {
				every { isLocalNetworkActive } returns true
				every { activeNetworkInterface } returns null
			},
            pokeServer
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
