package com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters;

import com.lasthopesoftware.bluewater.client.library.items.IItem;

/**
 * Created by david on 11/26/15.
 */
public interface IFileListParameterProvider<TItem extends IItem> {
	String[] getFileListParameters(TItem item);
}
