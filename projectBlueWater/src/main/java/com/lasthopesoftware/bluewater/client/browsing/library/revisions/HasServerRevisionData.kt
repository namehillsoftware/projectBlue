package com.lasthopesoftware.bluewater.client.browsing.library.revisions

import com.namehillsoftware.handoff.promises.Promise
import java.net.URL

interface HasServerRevisionData {
	fun getOrSetRevisionData(url: URL, setter: (URL) -> Promise<Int>): Promise<Int>
}
