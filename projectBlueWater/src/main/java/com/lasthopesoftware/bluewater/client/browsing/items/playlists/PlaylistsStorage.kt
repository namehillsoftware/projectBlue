package com.lasthopesoftware.bluewater.client.browsing.items.playlists

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.StandardRequest
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise

class PlaylistsStorage(private val libraryConnections: ProvideLibraryConnections) : StorePlaylists {
	override fun promiseStoredPlaylist(libraryId: LibraryId, playlistPath: String, playlist: List<ServiceFile>): Promise<*> {
		return libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventually { connectionProvider ->
				connectionProvider?.run {
					promiseResponse("Playlists", "Add?Type=Playlist&Path=$playlistPath&CreateMode=Overwrite")
						.then { it?.use { r -> r.body?.byteStream()?.use(StandardRequest::fromInputStream) } }
						.then { it?.items?.get("PlaylistID") }
						.eventually {
							it?.let { playlistId ->
								QueuedPromise(MessageWriter{ playlist.map { sf -> sf.key }.joinToString(",") }, ThreadPools.compute)
									.eventually { keys ->
										promiseResponse(
											"Playlist",
											"AddFiles?PlaylistType=ID&Playlist=$playlistId&Keys=$keys"
										)
									}
							}.keepPromise()
						}
				}.keepPromise()
			}
	}
}
