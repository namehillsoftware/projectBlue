package com.lasthopesoftware.bluewater.client.playback.nowplaying.intent.GivenTheActiveLibraryIdChanges

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.android.intents.IntentBuilder
import com.lasthopesoftware.bluewater.android.intents.safelyGetParcelableExtra
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.navigation.NowPlayingScreen
import com.lasthopesoftware.bluewater.client.destinationProperty
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.robolectric.Shadows

class WhenBuildingTheNowPlayingIntent : AndroidContext() {
	companion object {
		private var returnedIntent: Intent? = null
	}

	override fun before() {
		val intentBuilder = IntentBuilder(ApplicationProvider.getApplicationContext())
		intentBuilder.buildPendingNowPlayingIntent(LibraryId(559))
		val pendingIntent = intentBuilder.buildPendingNowPlayingIntent(LibraryId(844))
		val shadow = Shadows.shadowOf(pendingIntent)
		returnedIntent = shadow.savedIntent
	}

	@Test
	fun `then the returned intent has the correct library`() {
		assertThat(returnedIntent?.safelyGetParcelableExtra<NowPlayingScreen>(destinationProperty)).isEqualTo(
			NowPlayingScreen(
				LibraryId(844)
			)
		)
	}
}
