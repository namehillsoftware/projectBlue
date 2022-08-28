package com.lasthopesoftware.bluewater.client.browsing.files.cached.repository

import android.database.sqlite.SQLiteDatabase
import androidx.annotation.Keep
import com.lasthopesoftware.bluewater.repository.Entity
import com.lasthopesoftware.bluewater.repository.IEntityCreator
import com.lasthopesoftware.bluewater.repository.IEntityUpdater

@Keep
class CachedFile : Entity, IEntityCreator, IEntityUpdater {
    var id: Long = 0

    /**
     * @return the library
     */
    var libraryId = 0

    /**
     * @return the cacheName
     */
    var cacheName: String? = null

    /**
     * @return the lastAccessedTime
     */
    var lastAccessedTime: Long = 0

    /**
     * @return the createdTime
     */
    var createdTime: Long = 0

    /**
     * @return the uniqueKey
     */
    var uniqueKey: String? = null

    /**
     * @return the fileName
     */
    var fileName: String? = null

    /**
     * @return the fileSize
     */
    var fileSize: Long = 0

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE `CachedFile` (`cacheName` VARCHAR , `createdTime` BIGINT , `fileName` VARCHAR , `fileSize` BIGINT , `id` INTEGER PRIMARY KEY AUTOINCREMENT , `lastAccessedTime` BIGINT , `libraryId` INTEGER , `uniqueKey` VARCHAR ,  UNIQUE (`fileName`), UNIQUE (`cacheName`,`libraryId`,`uniqueKey`) ) ")
        db.execSQL("CREATE INDEX `CachedFile_lastAccessedTime_idx` ON `CachedFile` ( `lastAccessedTime` )")
        db.execSQL("CREATE INDEX `CachedFile_cacheName_idx` ON `CachedFile` ( `cacheName` )")
        db.execSQL("CREATE INDEX `CachedFile_createdTime_idx` ON `CachedFile` ( `createdTime` )")
    }

    override fun onUpdate(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    companion object {
        const val LIBRARY_ID = "libraryId"
        const val LAST_ACCESSED_TIME = "lastAccessedTime"
        const val UNIQUE_KEY = "uniqueKey"
        const val CACHE_NAME = "cacheName"
        const val FILE_NAME = "fileName"
        const val FILE_SIZE = "fileSize"
        const val CREATED_TIME = "createdTime"
        const val tableName = "CachedFile"
    }
}
