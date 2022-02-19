package com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters

import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist

/**
 * Created by david on 11/26/15.
 */
interface IFileListParameterProvider {
    fun getFileListParameters(itemId: ItemId): Array<String>
    fun getFileListParameters(playlist: Playlist): Array<String>
}
