package com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters

import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist

object FileListParameters : IFileListParameterProvider {
    override fun getFileListParameters(itemId: ItemId): Array<String> = arrayOf(
		"Browse/Files",
		"ID=${itemId.id}",
		"Version=2"
	)

    override fun getFileListParameters(playlist: Playlist): Array<String> =
		arrayOf("Playlist/Files", "Playlist=" + playlist.key)

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
