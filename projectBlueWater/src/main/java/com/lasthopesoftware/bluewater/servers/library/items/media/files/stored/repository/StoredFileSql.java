package com.lasthopesoftware.bluewater.servers.library.items.media.files.stored.repository;

/**
 * Created by david on 12/13/15.
 */
public class StoredFileSql {
	public final String selectStoredFileById = "SELECT * FROM STOREDFILE WHERE ID = @ID";
}
