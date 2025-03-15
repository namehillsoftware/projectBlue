package com.lasthopesoftware.bluewater.client.access

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion
import com.namehillsoftware.handoff.promises.Promise
import java.io.InputStream

interface RemoteLibraryAccess {
	fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>>
	fun promiseFilePropertyUpdate(
		serviceFile: ServiceFile,
		property: String,
		value: String,
		isFormatted: Boolean
	): Promise<Unit>

	fun promiseItems(itemId: ItemId?): Promise<List<Item>>
	fun promiseAudioPlaylistPaths(): Promise<List<String>>
	fun promiseStoredPlaylist(playlistPath: String, playlist: List<ServiceFile>): Promise<*>
	fun promiseIsReadOnly(): Promise<Boolean>
	fun promiseServerVersion(): Promise<SemanticVersion?>
	fun promiseRevision(): Promise<Int?>
	fun promiseFile(serviceFile: ServiceFile): Promise<InputStream>
	fun promisePlaystatsUpdate(serviceFile: ServiceFile): Promise<*>
	fun promiseFiles(): Promise<List<ServiceFile>>
	fun promiseFiles(query: String): Promise<List<ServiceFile>>
	fun promiseFiles(itemId: ItemId): Promise<List<ServiceFile>>
	fun promiseFiles(playlistId: PlaylistId): Promise<List<ServiceFile>>
	fun promiseFileStringList(option: FileListParameters.Options, vararg params: String): Promise<String>
	fun promiseFiles(option: FileListParameters.Options, vararg params: String): Promise<List<ServiceFile>>
	fun promiseImageBytes(serviceFile: ServiceFile): Promise<ByteArray>
	fun promiseImageBytes(itemId: ItemId): Promise<ByteArray>
}
