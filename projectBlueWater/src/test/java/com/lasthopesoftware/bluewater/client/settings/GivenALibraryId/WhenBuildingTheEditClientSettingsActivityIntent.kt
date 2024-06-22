package com.lasthopesoftware.bluewater.client.settings.GivenALibraryId

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.bluewater.android.intents.BuildIntents
import com.lasthopesoftware.bluewater.android.intents.IntentBuilder
import com.lasthopesoftware.bluewater.client.EntryActivity
import com.lasthopesoftware.bluewater.client.browsing.BrowserViewDependencies
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.navigation.ConnectionSettingsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.Destination
import com.lasthopesoftware.bluewater.client.destinationProperty
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.shared.cls
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WhenBuildingTheEditClientSettingsActivityIntent {

    companion object {
        private var returnedIntent: Intent? = null
    }

    @Before
    fun before() {
        val editClientSettingsActivityIntentBuilder = IntentBuilder(ApplicationProvider.getApplicationContext(), cls<FakeActivity>())
        returnedIntent = editClientSettingsActivityIntentBuilder.buildLibrarySettingsIntent(LibraryId(13))
    }

    @Test
    fun thenTheIdInTheIntentIsSetToTheLibraryId() {
        assertThat(
            returnedIntent?.getParcelableExtra(destinationProperty, cls<ConnectionSettingsScreen>())?.libraryId
        ).isEqualTo(LibraryId(13))
    }

    @Test
    fun thenTheReturnedIntentActivityIsCorrect() {
        assertThat(returnedIntent!!.component!!.className)
            .isEqualTo(FakeActivity::class.java.name)
    }

	private class FakeActivity : EntryActivity() {
		@Composable
		override fun Application(
			browserViewDependencies: BrowserViewDependencies,
			permissionsDependencies: PermissionsDependencies,
			initialDestination: Destination?
		) {}

		override val intentBuilder: BuildIntents
			get() = IntentBuilder(this, javaClass)
	}
}
