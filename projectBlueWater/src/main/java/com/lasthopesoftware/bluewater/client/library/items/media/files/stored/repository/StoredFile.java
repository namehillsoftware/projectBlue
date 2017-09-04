package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;

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

	public void setId(int id) { this.id = id; }

	public int getId() {
		return id;
	}

	public int getLibraryId() {
		return libraryId;
	}

	public void setLibraryId(int libraryId) {
		this.libraryId = libraryId;
	}

	public int getStoredMediaId() {
		return storedMediaId;
	}

	public void setStoredMediaId(int storedMediaId) {
		this.storedMediaId = storedMediaId;
	}

	public int getServiceId() {
		return serviceId;
	}

	public void setServiceId(int serviceId) {
		this.serviceId = serviceId;
	}

	public boolean isDownloadComplete() {
		return isDownloadComplete;
	}

	public void setIsDownloadComplete(boolean isDownloadComplete) {
		this.isDownloadComplete = isDownloadComplete;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isOwner() {
		return isOwner;
	}

	public void setIsOwner(boolean isOwner) {
		this.isOwner = isOwner;
	}
}