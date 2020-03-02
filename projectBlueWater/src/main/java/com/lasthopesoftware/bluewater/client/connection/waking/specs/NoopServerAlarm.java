package com.lasthopesoftware.bluewater.client.connection.waking.specs;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.connection.waking.WakeLibraryServer;
import com.namehillsoftware.handoff.promises.Promise;

import org.jetbrains.annotations.NotNull;

import kotlin.Unit;

public class NoopServerAlarm implements WakeLibraryServer {
	@NotNull
	@Override
	public Promise<Unit> awakeLibraryServer(@NotNull LibraryId libraryId) {
		return new Promise<>(Unit.INSTANCE);
	}
}
