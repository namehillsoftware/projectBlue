package com.lasthopesoftware.bluewater.client.library.access.specs;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.Collection;

public class FakeLibraryProvider implements ILibraryProvider {
	private final Collection<Library> libraries;

	public FakeLibraryProvider(Collection<Library> libraries) {
		this.libraries = libraries;
	}

	@Override
	public Promise<Library> getLibrary(LibraryId libraryId) {
		return new Promise<>(Stream.of(libraries).filter(l -> l.getLibraryId().equals(libraryId)).findFirst().get());
	}

	@Override
	public Promise<Collection<Library>> getAllLibraries() {
		return new Promise<>(libraries);
	}
}
