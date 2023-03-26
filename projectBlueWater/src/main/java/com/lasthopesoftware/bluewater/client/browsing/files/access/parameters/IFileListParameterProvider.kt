package com.lasthopesoftware.bluewater.client.browsing.files.access.parameters

import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId

interface IFileListParameterProvider {
    fun getFileListParameters(): Array<String>
    fun getFileListParameters(itemId: ItemId): Array<String>
    fun getFileListParameters(playlistId: PlaylistId): Array<String>
}
