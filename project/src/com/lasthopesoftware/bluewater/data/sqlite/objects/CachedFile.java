package com.lasthopesoftware.bluewater.data.sqlite.objects;

import java.util.Date;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "CachedFile")
public class CachedFile {

	public static final String LIBRARY_ID = "libraryId";
	public static final String LAST_ACCESSED_TIME = "lastAccessedTime";
	public static final String UNIQUE_KEY = "uniqueKey";
	public static final String CACHE_NAME = "cacheName";
	public static final String FILE_NAME = "fileName";
	public static final String FILE_SIZE = "fileSize";
	
	@DatabaseField(generatedId = true)
	private int id;
	
	@DatabaseField(foreign = true, columnName = LIBRARY_ID, uniqueCombo = true)
	private Library library;
	
	@DatabaseField(uniqueCombo = true, index = true)
	private String cacheName;
	
	@DatabaseField(index = true)
	private Date lastAccessedTime;
	
	@DatabaseField(uniqueCombo = true)
	private String uniqueKey;
	
	@DatabaseField(unique = true)
	private String fileName;
	
	@DatabaseField()
	private long fileSize;

	/**
	 * @return the library
	 */
	public final Library getLibrary() {
		return library;
	}

	/**
	 * @param library the library to set
	 */
	public final void setLibrary(Library library) {
		this.library = library;
	}

	/**
	 * @return the lastAccessedTime
	 */
	public final Date getLastAccessedTime() {
		return lastAccessedTime;
	}

	/**
	 * @param lastAccessedTime the lastAccessedTime to set
	 */
	public final void setLastAccessedTime(Date lastAccessedTime) {
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
}
