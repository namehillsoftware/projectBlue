package com.lasthopesoftware.exceptions.GivenASocketIsClosedException

import com.lasthopesoftware.exceptions.isSocketClosedException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.SocketException

class `When Checking If It Is Socket Is Closed` {
	@Test
	fun `then it is`() {
		assertThat(SocketException("Socket is closed").isSocketClosedException()).isTrue()
	}
}
