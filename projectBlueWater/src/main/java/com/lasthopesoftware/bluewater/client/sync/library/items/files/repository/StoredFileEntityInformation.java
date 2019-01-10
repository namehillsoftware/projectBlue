package com.lasthopesoftware.bluewater.client.sync.library.items.files.repository;

/**
 * Created by david on 6/25/16.
 */
public class StoredFileEntityInformation {
	public static final String tableName = "StoredFiles";

	public static final String serviceIdColumnName = "serviceId";
	public static final String libraryIdColumnName = "libraryId";
	public static final String pathColumnName = "path";
	public static final String isOwnerColumnName = "isOwner";
	public static final String storedMediaIdColumnName = "storedMediaId";
	public static final String isDownloadCompleteColumnName = "isDownloadComplete";

	public static final String createTableSql = "CREATE TABLE `StoredFiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT , `isDownloadComplete` SMALLINT , `isOwner` SMALLINT , `libraryId` INTEGER , `path` VARCHAR , `serviceId` INTEGER , `storedMediaId` INTEGER ,  UNIQUE (`libraryId`,`serviceId`) ) ";
}
