package com.lasthopesoftware.bluewater.client.stored.library.items.files.repository

/**
 * Created by david on 6/25/16.
 */
object StoredFileEntityInformation {
    const val tableName = "StoredFiles"
    const val serviceIdColumnName = "serviceId"
    const val libraryIdColumnName = "libraryId"
    const val pathColumnName = "path"
    const val isOwnerColumnName = "isOwner"
    const val storedMediaIdColumnName = "storedMediaId"
    const val isDownloadCompleteColumnName = "isDownloadComplete"
    const val createTableSql =
        "CREATE TABLE `StoredFiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT , `isDownloadComplete` SMALLINT , `isOwner` SMALLINT , `libraryId` INTEGER , `path` VARCHAR , `serviceId` INTEGER , `storedMediaId` INTEGER ,  UNIQUE (`libraryId`,`serviceId`) ) "
}
