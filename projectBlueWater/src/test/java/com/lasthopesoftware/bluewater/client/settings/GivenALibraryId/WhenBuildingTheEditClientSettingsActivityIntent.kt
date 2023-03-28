package com.lasthopesoftware.bluewater.client.settings.GivenALibraryId

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.bluewater.client.browsing.BrowserActivity
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.libraryIdProperty
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
            returnedIntent!!.getIntExtra(
				libraryIdProperty,
                -1
            )
        ).isEqualTo(13)
    }

    @Test
    fun thenTheReturnedIntentIsEditClientSettingsActivity() {
        assertThat(returnedIntent!!.component!!.className)
            .isEqualTo(BrowserActivity::class.java.name)
    }
}
