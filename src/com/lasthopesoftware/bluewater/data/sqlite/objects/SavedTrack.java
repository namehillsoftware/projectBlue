package com.lasthopesoftware.bluewater.data.sqlite.objects;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "SAVED_TRACKS")
public class SavedTrack {
	@DatabaseField(columnName = "TRACK_ID")
	private int trackId;

//	@Override
//	public String[] getSqlColumns() {
//		return new String[] { "LIBRARY_ID", "TRACK_ID" };
//	}
//
//	@Override
//	public String[] getSqlColumnDefintions() {
//		return new String[] { "LIBRARY_ID INTEGER", "TRACK_ID INTEGER", "FOREIGN KEY (LIBRARY_ID) REFERENCES LIBRARY(ID)" };
//	}

	/**
	 * @return the trackId
	 */
	public int getTrackId() {
		return trackId;
	}

	/**
	 * @param trackId the trackId to set
	 */
	public void setTrackId(int trackId) {
		this.trackId = trackId;
	}
}
