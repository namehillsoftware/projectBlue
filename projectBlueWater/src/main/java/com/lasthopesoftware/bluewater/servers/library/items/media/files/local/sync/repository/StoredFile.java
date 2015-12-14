package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.repository;

public class StoredFile {

	public static final String tableName = "StoredFiles";

	public static final String serviceIdColumnName = "serviceId";
	public static final String libraryIdColumnName = "libraryId";
	public static final String pathColumnName = "path";
	public static final String isOwnerColumnName = "isOwner";
	public static final String storedMediaIdColumnName = "storedMediaId";
	public static final String isDownloadCompleteColumnName = "isDownloadComplete";

	private int id;
	
	private int libraryId;
	
	private int storedMediaId;

	private int serviceId;

	private boolean isDownloadComplete;

	private String path;

	private boolean isOwner;

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