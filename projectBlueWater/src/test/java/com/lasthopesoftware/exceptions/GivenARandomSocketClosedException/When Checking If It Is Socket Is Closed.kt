package com.lasthopesoftware.exceptions.GivenARandomSocketClosedException

import com.lasthopesoftware.exceptions.isSocketClosedException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException

class `When Checking If It Is Socket Is Closed` {
	@Test
	fun `then it is not`() {
		assertThat(IOException("Pretiumplatea Fuscemattis.").isSocketClosedException()).isFalse()
	}
}
