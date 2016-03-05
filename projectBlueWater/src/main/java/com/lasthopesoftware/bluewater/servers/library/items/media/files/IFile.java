package com.lasthopesoftware.bluewater.servers.library.items.media.files;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.shared.IIntKey;

public interface IFile extends IIntKey<IFile> {
	String getPlaybackUrl(ConnectionProvider connectionProvider);
	String[] getPlaybackParams();
}
