package com.lasthopesoftware.bluewater.client.servers.selection;

import com.namehillsoftware.handoff.promises.Promise;

/**
 * Created by david on 2/12/17.
 */

public interface ISelectedLibraryIdentifierProvider {
	Promise<Integer> getSelectedLibraryId();
}
