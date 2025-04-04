package com.lasthopesoftware.bluewater.client.stored.library.items.files.repository

/**
 * Created by david on 6/25/16.
 */
object StoredFileEntityInformation {
    const val tableName = "StoredFiles"
    const val serviceIdColumnName = "serviceId"
    const val libraryIdColumnName = "libraryId"
    const val uriColumnName = "uri"
    const val isOwnerColumnName = "isOwner"
    const val isDownloadCompleteColumnName = "isDownloadComplete"
    const val createTableSql =
        "CREATE TABLE IF NOT EXISTS `$tableName` (`id` INTEGER PRIMARY KEY AUTOINCREMENT , `isDownloadComplete` SMALLINT , `isOwner` SMALLINT , `libraryId` INTEGER , `uri` VARCHAR , `serviceId` VARCHAR ,  UNIQUE (`libraryId`,`serviceId`) ) "
}
