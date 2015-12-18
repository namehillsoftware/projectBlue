package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.cache.repository;

import android.database.sqlite.SQLiteDatabase;

import com.lasthopesoftware.bluewater.repository.IRepository;

public class CachedFile implements IRepository {

	public static final String LIBRARY_ID = "libraryId";
	public static final String LAST_ACCESSED_TIME = "lastAccessedTime";
	public static final String UNIQUE_KEY = "uniqueKey";
	public static final String CACHE_NAME = "cacheName";
	public static final String FILE_NAME = "fileName";
	public static final String FILE_SIZE = "fileSize";
	public static final String CREATED_TIME = "createdTime";
	public static final String tableName = "CachedFile";
	
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

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE `CachedFile` (`cacheName` VARCHAR , `createdTime` BIGINT , `fileName` VARCHAR , `fileSize` BIGINT , `id` INTEGER PRIMARY KEY AUTOINCREMENT , `lastAccessedTime` BIGINT , `libraryId` INTEGER , `uniqueKey` VARCHAR ,  UNIQUE (`fileName`), UNIQUE (`cacheName`,`libraryId`,`uniqueKey`) ) ");
		db.execSQL("CREATE INDEX `CachedFile_lastAccessedTime_idx` ON `CachedFile` ( `lastAccessedTime` )");
		db.execSQL("CREATE INDEX `CachedFile_cacheName_idx` ON `CachedFile` ( `cacheName` )");
		db.execSQL("CREATE INDEX `CachedFile_createdTime_idx` ON `CachedFile` ( `createdTime` )");
	}

	@Override
	public void onUpdate(SQLiteDatabase db, int oldVersion, int newVersion) {

	}
}
