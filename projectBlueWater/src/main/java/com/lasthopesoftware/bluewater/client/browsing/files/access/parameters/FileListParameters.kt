package com.lasthopesoftware.bluewater.client.browsing.files.access.parameters

import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId

object FileListParameters : IFileListParameterProvider {
	override fun getFileListParameters(): Array<String> = arrayOf(
		"Browse/Files",
		"Version=2"
	)

	override fun getFileListParameters(itemId: ItemId): Array<String> = arrayOf(
		"Browse/Files",
		"ID=${itemId.id}",
		"Version=2"
	)

    override fun getFileListParameters(playlistId: PlaylistId): Array<String> =
		arrayOf("Playlist/Files", "Playlist=" + playlistId.id)

	override fun getFileListParameters(query: String) =
		arrayOf("Files/Search", "Query=[Media Type]=[Audio] $query")

    enum class Options {
        None, Shuffled
    }

    object Helpers {
        fun processParams(option: Options, vararg params: String): Array<String> {
            val newParams = mutableListOf(*params)
            newParams.add("Action=Serialize")
            if (option == Options.Shuffled) newParams.add("Shuffle=1")
            return newParams.toTypedArray()
        }
    }
}
