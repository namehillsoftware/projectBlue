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

class WhenWakingALibraryServer {

	private val expectedPokedMachineAddresses = arrayOf(
		MachineAddress("local-address", "AB-E0-9F-24-F5"),
		MachineAddress("local-address", "99-53-7F-2C-A1"),
		MachineAddress("second-local-address", "AB-E0-9F-24-F5"),
		MachineAddress("second-local-address", "99-53-7F-2C-A1"),
		MachineAddress("remote-address", "AB-E0-9F-24-F5"),
		MachineAddress("remote-address", "99-53-7F-2C-A1")
	)

	private val mut by lazy {
		val lookupServers = mockk<LookupServers>().apply {
			every { promiseServerInformation(any()) } returns Promise(
				ServerInfo(
					5001,
					5002,
					setOf("remote-address"),
					setOf("local-address", "second-local-address"),
					setOf("AB-E0-9F-24-F5", "99-53-7F-2C-A1"),
					ByteArray(0)
				)
			)
		}

		val pokeServer = mockk<PokeServer>().apply {
			every { promiseWakeSignal(any()) } returns Unit.toPromise()

			every { promiseWakeSignal(any()) } answers {
				pokedMachineAddresses.add(firstArg())
				Unit.toPromise()
			}
		}

		val serverAlarm = ServerAlarm(
            lookupServers,
			mockk(relaxed = true),
            pokeServer
        )

		serverAlarm
	}

	private val pokedMachineAddresses: MutableList<MachineAddress> = ArrayList()

	@BeforeAll
	fun before() {
		mut.awakeLibraryServer(LibraryId(14)).toExpiringFuture().get()
	}

	@Test
	fun `then the machine is alerted at all endpoints`() {
		assertThat(pokedMachineAddresses).containsExactlyInAnyOrder(*expectedPokedMachineAddresses)
	}
}
