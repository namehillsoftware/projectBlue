package com.lasthopesoftware.bluewater.client.connection.waking.GivenALibrary.AndALocalConnection

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.waking.MachineAddress
import com.lasthopesoftware.bluewater.client.connection.waking.ServerAlarm
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.InterfaceAddress
import java.net.NetworkInterface

class WhenWakingALibraryServer {

	private val localNetwork by lazy {
		mockk<NetworkInterface> {
			every { interfaceAddresses } returns listOf(mockk<InterfaceAddress> {
				every { broadcast } returns mockk {
					every { hostAddress } returns "test-server"
				}
			})
		}
	}

	private val expectedPokedMachineAddresses by lazy {
		listOf(
			MachineAddress("local-address", "AB-E0-9F-24-F5"),
			MachineAddress("second-local-address", "AB-E0-9F-24-F5"),
			MachineAddress("remote-address", "AB-E0-9F-24-F5"),
			MachineAddress("255.255.255.255", "AB-E0-9F-24-F5"),
			MachineAddress("test-server", "AB-E0-9F-24-F5"),
		)
	}

	private val mut by lazy {
		ServerAlarm(
			mockk {
				every { promiseServerInformation(any()) } returns Promise(
					ServerInfo(
						5001,
						5002,
						setOf("remote-address"),
						setOf("local-address", "second-local-address"),
						setOf("AB-E0-9F-24-F5"),
						ByteArray(0)
					)
				)
			},
			mockk {
				every { isLocalNetworkActive } returns true
				every { activeNetworkInterface } answers { localNetwork }
			},
			mockk {
				every { promiseWakeSignal(any()) } returns Unit.toPromise()

				every { promiseWakeSignal(any()) } answers {
					pokedMachineAddresses.add(firstArg())
					Unit.toPromise()
				}
			}
        )
	}

	private val pokedMachineAddresses: MutableList<MachineAddress> = ArrayList()

	@BeforeAll
	fun before() {
		mut.awakeLibraryServer(LibraryId(14)).toExpiringFuture().get()
	}

	@Test
	fun `then the machine is alerted at all endpoints`() {
		assertThat(pokedMachineAddresses).containsExactlyInAnyOrder(*expectedPokedMachineAddresses.toTypedArray())
	}
}
