package com.lasthopesoftware.bluewater.client.settings.GivenALibraryId

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.settings.EditClientSettingsActivity
import com.lasthopesoftware.bluewater.client.settings.EditClientSettingsActivityIntentBuilder
import com.lasthopesoftware.resources.intents.IntentFactory
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
        val editClientSettingsActivityIntentBuilder = EditClientSettingsActivityIntentBuilder(
            IntentFactory(ApplicationProvider.getApplicationContext())
        )
        returnedIntent = editClientSettingsActivityIntentBuilder.buildIntent(LibraryId(13))
    }

    @Test
    fun thenTheIdInTheIntentIsSetToTheLibraryId() {
        assertThat(
            returnedIntent!!.getIntExtra(
				EditClientSettingsActivity.serverIdExtra,
                -1
            )
        ).isEqualTo(13)
    }

    @Test
    fun thenTheReturnedIntentIsEditClientSettingsActivity() {
        assertThat(returnedIntent!!.component!!.className)
            .isEqualTo(EditClientSettingsActivity::class.java.name)
    }
}
