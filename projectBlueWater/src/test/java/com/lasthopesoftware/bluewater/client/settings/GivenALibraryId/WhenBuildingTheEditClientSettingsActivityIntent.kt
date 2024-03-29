package com.lasthopesoftware.bluewater.client.settings.GivenALibraryId

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.bluewater.client.browsing.BrowserActivity
import com.lasthopesoftware.bluewater.client.browsing.destinationProperty
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.navigation.ConnectionSettingsScreen
import com.lasthopesoftware.bluewater.shared.android.intents.IntentBuilder
import org.assertj.core.api.Assertions.*
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
        val editClientSettingsActivityIntentBuilder = IntentBuilder(ApplicationProvider.getApplicationContext())
        returnedIntent = editClientSettingsActivityIntentBuilder.buildLibrarySettingsIntent(LibraryId(13))
    }

    @Test
    fun thenTheIdInTheIntentIsSetToTheLibraryId() {
        assertThat(
            (returnedIntent?.getParcelableExtra(destinationProperty) as? ConnectionSettingsScreen)?.libraryId
        ).isEqualTo(LibraryId(13))
    }

    @Test
    fun thenTheReturnedIntentActivityIsCorrect() {
        assertThat(returnedIntent!!.component!!.className)
            .isEqualTo(BrowserActivity::class.java.name)
    }
}
