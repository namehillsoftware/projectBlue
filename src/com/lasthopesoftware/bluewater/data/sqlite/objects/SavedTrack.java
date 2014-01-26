package com.lasthopesoftware.bluewater.data.sqlite.objects;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "SAVED_TRACKS")
public class SavedTrack {
	
	@DatabaseField(generatedId = true)
	private int id;
	@DatabaseField(columnName = "TRACK_ID")
	private int trackId;
	@DatabaseField(foreign = true)
	private Library library;

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
