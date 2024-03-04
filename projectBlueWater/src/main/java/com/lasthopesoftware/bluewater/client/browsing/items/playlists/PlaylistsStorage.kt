package com.lasthopesoftware.bluewater.client.browsing.items.playlists

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.StandardResponse
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import xmlwise.Xmlwise

class PlaylistsStorage(private val libraryConnections: ProvideLibraryConnections) : StorePlaylists {
	override fun promiseAudioPlaylistPaths(libraryId: LibraryId): Promise<List<String>> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventually { connectionProvider ->
				connectionProvider
					?.promiseResponse("Playlists/List", "IncludeMediaTypes=1")
					?.then { response ->
						response.body
							?.use { body -> Xmlwise.createXml(body.string()) }
							?.let { xml ->
								xml
									.get("Item")
									.mapNotNull { itemXml ->
										itemXml
											.takeIf {
												it.get("Field")
													.any { el -> el.getAttribute("Name") == "MediaTypes" && el.value == "Audio" }
											}
											?.get("Field")
											?.firstOrNull { el -> el.getAttribute("Name") == "Path" }
											?.value
									}
							}
							?: emptyList()
					}
					.keepPromise(emptyList())
			}

	override fun promiseStoredPlaylist(libraryId: LibraryId, playlistPath: String, playlist: List<ServiceFile>): Promise<*> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventually { connectionProvider ->
				connectionProvider?.run {
					promiseResponse("Playlists/Add", "Type=Playlist", "Path=$playlistPath", "CreateMode=Overwrite")
						.then { it -> it?.use { r -> r.body?.byteStream()?.use(StandardResponse::fromInputStream) } }
						.then { it -> it?.items?.get("PlaylistID") }
						.eventually {
							it?.let { playlistId ->
								QueuedPromise(MessageWriter{ playlist.map { sf -> sf.key }.joinToString(",") }, ThreadPools.compute)
									.eventually { keys ->
										promiseResponse(
											"Playlist/AddFiles",
											"PlaylistType=ID",
											"Playlist=$playlistId",
											"Keys=$keys",
										)
									}
							}.keepPromise()
						}
				}.keepPromise()
			}
}
