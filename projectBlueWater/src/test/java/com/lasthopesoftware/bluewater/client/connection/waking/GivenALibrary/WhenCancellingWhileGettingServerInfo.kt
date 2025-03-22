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
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

class WhenCancellingWhileGettingServerInfo {

	private val mut by lazy {
		val lookupServers = mockk<LookupServers>().apply {
			every { promiseServerInformation(any()) } returns Promise<ServerInfo> { m ->
				m.awaitCancellation {
					m.sendRejection(CancellationException("CANCELLED!"))
				}
			}
		}

		val pokeServer = mockk<PokeServer>().apply {
			every { promiseWakeSignal(any()) } answers {
				Unit.toPromise()
			}

			every { promiseWakeSignal(any()) } answers {
				pokedMachineAddresses.add(firstArg())
				Unit.toPromise()
			}
		}

		val serverAlarm = ServerAlarm(
            lookupServers,
			mockk(),
            pokeServer
        )

		serverAlarm
	}

	private var cancellationException: CancellationException? = null
	private val pokedMachineAddresses: MutableList<MachineAddress> = ArrayList()

	@BeforeAll
	fun act() {
		val awakeLibraryServer = mut.awakeLibraryServer(LibraryId(14))
		awakeLibraryServer.cancel()
		try {
			awakeLibraryServer.toExpiringFuture()[5, TimeUnit.SECONDS]
		} catch (ee: ExecutionException) {
			cancellationException = ee.cause as? CancellationException ?: throw ee
		}
	}

	@Test
	fun `then the machine is alerted at all endpoints`() {
		assertThat(pokedMachineAddresses).isEmpty()
	}

	@Test
	fun `then getting server info is cancelled`() {
		assertThat(cancellationException?.message).isEqualTo("CANCELLED!")
	}
}
