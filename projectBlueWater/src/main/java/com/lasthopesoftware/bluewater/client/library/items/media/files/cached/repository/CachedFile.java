package com.lasthopesoftware.bluewater.client.library.items.media.files.cached.repository;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Keep;
import com.lasthopesoftware.bluewater.repository.IEntityCreator;
import com.lasthopesoftware.bluewater.repository.IEntityUpdater;

@Keep
public class CachedFile implements IEntityCreator, IEntityUpdater {

	public static final String LIBRARY_ID = "libraryId";
	public static final String LAST_ACCESSED_TIME = "lastAccessedTime";
	public static final String UNIQUE_KEY = "uniqueKey";
	public static final String CACHE_NAME = "cacheName";
	public static final String FILE_NAME = "fileName";
	public static final String FILE_SIZE = "fileSize";
	public static final String CREATED_TIME = "createdTime";
	public static final String tableName = "CachedFile";
	
	private long id;
	
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
	public final CachedFile setLibraryId(int libraryId) {
		this.libraryId = libraryId;
		return this;
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
	public final CachedFile setLastAccessedTime(long lastAccessedTime) {
		this.lastAccessedTime = lastAccessedTime;
		return this;
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
	public final CachedFile setFileName(String fileName) {
		this.fileName = fileName;
		return this;
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
	public final CachedFile setFileSize(long fileSize) {
		this.fileSize = fileSize;
		return this;
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
	public final CachedFile setUniqueKey(String uniqueKey) {
		this.uniqueKey = uniqueKey;
		return this;
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
	public final CachedFile setCacheName(String cacheName) {
		this.cacheName = cacheName;
		return this;
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
	public final CachedFile setCreatedTime(long createdTime) {
		this.createdTime = createdTime;
		return this;
	}

	public long getId() {
		return id;
	}

	public CachedFile setId(long id) {
		this.id = id;
		return this;
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
