package com.lasthopesoftware.bluewater.client.connection.requests

import com.namehillsoftware.handoff.promises.Promise
import java.net.URL

interface HttpPromiseClient {
	fun promiseResponse(url: URL): Promise<HttpResponse>
}
