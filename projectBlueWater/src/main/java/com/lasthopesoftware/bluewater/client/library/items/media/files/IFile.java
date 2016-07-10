package com.lasthopesoftware.bluewater.client.library.items.media.files;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.shared.IIntKey;

public interface IFile extends IIntKey<IFile> {
	String getPlaybackUrl(ConnectionProvider connectionProvider);
	String[] getPlaybackParams();
}
