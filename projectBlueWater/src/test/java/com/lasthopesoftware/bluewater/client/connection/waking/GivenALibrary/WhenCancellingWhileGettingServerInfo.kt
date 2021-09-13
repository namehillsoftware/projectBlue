package com.lasthopesoftware.bluewater.client.connection.waking.GivenALibrary

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.waking.AlarmConfiguration
import com.lasthopesoftware.bluewater.client.connection.waking.MachineAddress
import com.lasthopesoftware.bluewater.client.connection.waking.PokeServer
import com.lasthopesoftware.bluewater.client.connection.waking.ServerAlarm
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

class WhenCancellingWhileGettingServerInfo {

	companion object {
		private var cancellationException: CancellationException? = null
		private val pokedMachineAddresses: MutableList<MachineAddress> = ArrayList()

		@BeforeClass
		@JvmStatic
		fun before() {
			val lookupServers = mockk<LookupServers>()
			every { lookupServers.promiseServerInformation(any()) } returns Promise<ServerInfo?> { m ->
				m.cancellationRequested {
					m.sendRejection(CancellationException("CANCELLED!"))
				}
			}

			val pokeServer = mockk<PokeServer>()
			every { pokeServer.promiseWakeSignal(any(), any(), any()) } answers {
				Unit.toPromise()
			}

			every { pokeServer.promiseWakeSignal(any(), 10, Duration.standardHours(10)) } answers {
				pokedMachineAddresses.add(firstArg())
				Unit.toPromise()
			}

			val serverAlarm = ServerAlarm(
				lookupServers,
				pokeServer,
				AlarmConfiguration(10, Duration.standardHours(10))
			)

			val awakeLibraryServer = serverAlarm.awakeLibraryServer(LibraryId(14))
			awakeLibraryServer.cancel()
			try {
				awakeLibraryServer.toFuture()[5, TimeUnit.SECONDS]
			} catch (ee: ExecutionException) {
				cancellationException = ee.cause as? CancellationException ?: throw ee
			}
		}
	}

	@Test
	fun thenTheMachineIsAlertedAtAllEndpoints() {
		assertThat(pokedMachineAddresses).isEmpty()
	}

	@Test
	fun thenGettingServerInfoIsCancelled() {
		assertThat(cancellationException?.message).isEqualTo("CANCELLED!")
	}
}
