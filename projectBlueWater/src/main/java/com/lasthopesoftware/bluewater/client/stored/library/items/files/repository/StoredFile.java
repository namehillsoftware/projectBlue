package com.lasthopesoftware.bluewater.client.stored.library.items.files.repository;

import androidx.annotation.Keep;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;

import java.util.Objects;

@Keep
public final class StoredFile {

	private int id;
	private int libraryId;
	private int storedMediaId;
	private int serviceId;
	private boolean isDownloadComplete;
	private String path;
	private boolean isOwner;

	public StoredFile() {}

	public StoredFile(Library library, int storedMediaId, ServiceFile serviceFile, String path, boolean isOwner) {
		this.libraryId = library.getId();
		this.storedMediaId = storedMediaId;
		this.serviceId = serviceFile.getKey();
		this.path = path;
		this.isOwner = isOwner;
	}

	public StoredFile setId(int id) {
		this.id = id;
		return this;
	}

	public int getId() {
		return id;
	}

	public int getLibraryId() {
		return libraryId;
	}

	public StoredFile setLibraryId(int libraryId) {
		this.libraryId = libraryId;
		return this;
	}

	public int getStoredMediaId() {
		return storedMediaId;
	}

	public StoredFile setStoredMediaId(int storedMediaId) {
		this.storedMediaId = storedMediaId;
		return this;
	}

	public int getServiceId() {
		return serviceId;
	}

	public StoredFile setServiceId(int serviceId) {
		this.serviceId = serviceId;
		return this;
	}

	public boolean isDownloadComplete() {
		return isDownloadComplete;
	}

	public StoredFile setIsDownloadComplete(boolean isDownloadComplete) {
		this.isDownloadComplete = isDownloadComplete;
		return this;
	}

	public String getPath() {
		return path;
	}

	public StoredFile setPath(String path) {
		this.path = path;
		return this;
	}

	public boolean isOwner() {
		return isOwner;
	}

	public StoredFile setIsOwner(boolean isOwner) {
		this.isOwner = isOwner;
		return this;
	}


	@Override
	public final boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		StoredFile that = (StoredFile) o;
		return libraryId == that.libraryId &&
			serviceId == that.serviceId;
	}

	@Override
	public final int hashCode() {
		return Objects.hash(libraryId, serviceId);
	}

	@Override
	public String toString() {
		return "StoredFile{" +
			"id=" + id +
			", libraryId=" + libraryId +
			", storedMediaId=" + storedMediaId +
			", serviceId=" + serviceId +
			", isDownloadComplete=" + isDownloadComplete +
			", path='" + path + '\'' +
			", isOwner=" + isOwner +
			'}';
	}
}
