package com.lasthopesoftware.bluewater.client.library.repository;

import java.util.Objects;

public class LibraryId {
	private final int id;

	public LibraryId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LibraryId libraryId = (LibraryId) o;
		return id == libraryId.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "LibraryId{" +
			"id=" + id +
			'}';
	}
}
