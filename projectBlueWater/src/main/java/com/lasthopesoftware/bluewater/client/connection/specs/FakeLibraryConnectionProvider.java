package com.lasthopesoftware.bluewater.client.connection.specs;

import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise;

import org.jetbrains.annotations.NotNull;

public class FakeLibraryConnectionProvider implements ProvideLibraryConnections {
	@NotNull
	@Override
	public ProgressingPromise<BuildingConnectionStatus, IConnectionProvider> promiseLibraryConnection(@NotNull LibraryId libraryId) {
		return new ProgressingPromise<>(new FakeConnectionProvider());
	}

	@NotNull
	@Override
	public ProgressingPromise<BuildingConnectionStatus, IConnectionProvider> promiseTestedLibraryConnection(@NotNull LibraryId libraryId) {
		return new ProgressingPromise<>(new FakeConnectionProvider());
	}
}
