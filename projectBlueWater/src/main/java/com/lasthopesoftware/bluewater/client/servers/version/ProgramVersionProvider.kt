package com.lasthopesoftware.bluewater.client.servers.version

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.StandardRequest
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class ProgramVersionProvider(private val connectionProvider: IConnectionProvider) : IProgramVersionProvider {

	private var serverVersion: SemanticVersion? = null

	override fun promiseServerVersion(): Promise<SemanticVersion?> =
		serverVersion?.toPromise() ?: connectionProvider.promiseResponse("Alive")
			.then { response ->
				response.body
					?.use { body -> body.byteStream().use(StandardRequest::fromInputStream) }
					?.let { standardRequest -> standardRequest.items["ProgramVersion"] }
					?.let { semVerString ->
						val semVerParts = semVerString.split("\\.").toTypedArray()
						var major = 0
						var minor = 0
						var patch = 0
						if (semVerParts.size > 0) major = semVerParts[0].toInt()
						if (semVerParts.size > 1) minor = semVerParts[1].toInt()
						if (semVerParts.size > 2) patch = semVerParts[2].toInt()
						serverVersion = SemanticVersion(major, minor, patch)
						serverVersion
					}
			}
}
