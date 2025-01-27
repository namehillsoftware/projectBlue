package com.lasthopesoftware.bluewater.client.browsing.items.playlists

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.io.promiseStandardResponse
import com.lasthopesoftware.resources.io.promiseStringBody
import com.lasthopesoftware.resources.io.promiseXmlDocument
import com.namehillsoftware.handoff.promises.Promise

class PlaylistsStorage(private val libraryConnections: ProvideLibraryConnections) : StorePlaylists {
	override fun promiseAudioPlaylistPaths(libraryId: LibraryId): Promise<List<String>> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventually { connectionProvider ->
				connectionProvider
					?.promiseResponse("Playlists/List", "IncludeMediaTypes=1")
					?.promiseStringBody()
					?.promiseXmlDocument()
					?.then { xml ->
						xml
							.getElementsByTag("Item")
							.mapNotNull { itemXml ->
								itemXml
									.takeIf {
										it.getElementsByTag("Field")
											.any { el -> el.attr("Name") == "MediaTypes" && el.ownText() == "Audio" }
									}
									?.getElementsByTag("Field")
									?.firstOrNull { el -> el.attr("Name") == "Path" }
									?.ownText()
							}
					}
					.keepPromise(emptyList())
			}

	override fun promiseStoredPlaylist(libraryId: LibraryId, playlistPath: String, playlist: List<ServiceFile>): Promise<*> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventually { connectionProvider ->
				connectionProvider?.run {
					promiseResponse("Playlists/Add", "Type=Playlist", "Path=$playlistPath", "CreateMode=Overwrite")
						.promiseStandardResponse()
						.then { it -> it.items["PlaylistID"] }
						.eventually {
							it?.let { playlistId ->
								ThreadPools.compute
									.preparePromise { playlist.map { sf -> sf.key }.joinToString(",") }
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
