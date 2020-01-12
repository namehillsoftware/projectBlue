package com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters;

import com.lasthopesoftware.bluewater.client.browsing.items.Item;
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist;

/**
 * Created by david on 11/26/15.
 */
public interface IFileListParameterProvider {
	String[] getFileListParameters(Item item);

	String[] getFileListParameters(Playlist playlist);
}
