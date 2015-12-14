package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.cache.repository;

public class CachedFile {

	public static final String LIBRARY_ID = "libraryId";
	public static final String LAST_ACCESSED_TIME = "lastAccessedTime";
	public static final String UNIQUE_KEY = "uniqueKey";
	public static final String CACHE_NAME = "cacheName";
	public static final String FILE_NAME = "fileName";
	public static final String FILE_SIZE = "fileSize";
	public static final String CREATED_TIME = "createdTime";
	
	private int id;
	
	private int libraryId;
	
	private String cacheName;
	
	private long lastAccessedTime;
	
	private long createdTime;
	
	private String uniqueKey;
	
	private String fileName;
	
	private long fileSize;

	/**
	 * @return the library
	 */
	public final int getLibraryId() {
		return libraryId;
	}

	/**
	 * @param libraryId the library to set
	 */
	public final void setLibraryId(int libraryId) {
		this.libraryId = libraryId;
	}

	/**
	 * @return the lastAccessedTime
	 */
	public final long getLastAccessedTime() {
		return lastAccessedTime;
	}

	/**
	 * @param lastAccessedTime the lastAccessedTime to set
	 */
	public final void setLastAccessedTime(long lastAccessedTime) {
		this.lastAccessedTime = lastAccessedTime;
	}

	/**
	 * @return the fileName
	 */
	public final String getFileName() {
		return fileName;
	}

	/**
	 * @param fileName the fileName to set
	 */
	public final void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * @return the fileSize
	 */
	public final long getFileSize() {
		return fileSize;
	}

	/**
	 * @param fileSize the fileSize to set
	 */
	public final void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	/**
	 * @return the uniqueKey
	 */
	public final String getUniqueKey() {
		return uniqueKey;
	}

	/**
	 * @param uniqueKey the uniqueKey to set
	 */
	public final void setUniqueKey(String uniqueKey) {
		this.uniqueKey = uniqueKey;
	}

	/**
	 * @return the cacheName
	 */
	public final String getCacheName() {
		return cacheName;
	}

	/**
	 * @param cacheName the cacheName to set
	 */
	public final void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	/**
	 * @return the createdTime
	 */
	public final long getCreatedTime() {
		return createdTime;
	}

	/**
	 * @param createdTime the createdTime to set
	 */
	public final void setCreatedTime(long createdTime) {
		this.createdTime = createdTime;
	}
}
