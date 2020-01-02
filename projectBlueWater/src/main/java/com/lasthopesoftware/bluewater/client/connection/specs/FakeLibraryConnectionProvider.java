package com.lasthopesoftware.bluewater.client.connection.specs;

import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class FakeLibraryConnectionProvider implements ProvideLibraryConnections {

	private final Map<LibraryId, IConnectionProvider> connectionProviderMap;

	public FakeLibraryConnectionProvider(Map<LibraryId, IConnectionProvider> connectionProviderMap) {
		this.connectionProviderMap = connectionProviderMap;
	}

	@NotNull
	@Override
	public ProgressingPromise<BuildingConnectionStatus, IConnectionProvider> promiseLibraryConnection(@NotNull LibraryId libraryId) {
		return new ProgressingPromise<>(connectionProviderMap.get(libraryId));
	}

	@NotNull
	@Override
	public ProgressingPromise<BuildingConnectionStatus, IConnectionProvider> promiseTestedLibraryConnection(@NotNull LibraryId libraryId) {
		return new ProgressingPromise<>(connectionProviderMap.get(libraryId));
	}
}
