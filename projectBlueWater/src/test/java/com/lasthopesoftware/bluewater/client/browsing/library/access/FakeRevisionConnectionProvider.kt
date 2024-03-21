package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple

open class FakeRevisionConnectionProvider : FakeConnectionProvider() {
    private var syncRevision = 0

	fun setSyncRevision(syncRevision: Int) {
        this.syncRevision = syncRevision
    }

    init {
        mapResponse(
            {
                FakeConnectionResponseTuple(
                    200,
                    ("<Response Status=\"OK\">" +
                            "<Item Name=\"Master\">1192</Item>" +
                            "<Item Name=\"Sync\">" + syncRevision + "</Item>" +
                            "<Item Name=\"LibraryStartup\">1501430846</Item>" +
                            "</Response>").toByteArray()
                )
            },
            "Library/GetRevision"
        )
    }
}
