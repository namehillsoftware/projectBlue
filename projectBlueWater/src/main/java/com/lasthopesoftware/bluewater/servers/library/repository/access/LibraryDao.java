package com.lasthopesoftware.bluewater.servers.library.repository.access;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;

import java.sql.SQLException;

/**
 * Created by david on 9/20/15.
 */
public class LibraryDao extends BaseDaoImpl<Library, Integer> {

	public LibraryDao(ConnectionSource connectionSource) throws SQLException {
		super(connectionSource, Library.class);
	}
}
