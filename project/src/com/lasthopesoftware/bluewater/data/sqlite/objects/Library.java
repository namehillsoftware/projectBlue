package com.lasthopesoftware.bluewater.data.sqlite.objects;

import java.util.ArrayList;
import java.util.Collection;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.service.objects.Files;

@DatabaseTable(tableName = "LIBRARIES")
public class Library {
	
	@DatabaseField(generatedId = true)
	private int id;
	
	// Remote connection fields
	@DatabaseField(canBeNull = false, columnDefinition = "VARCHAR(50)")
	private String libraryName;
	@DatabaseField(canBeNull = false, columnDefinition = "VARCHAR(30)")
	private String accessCode;
	@DatabaseField(columnDefinition = "VARCHAR(100)")
	private String authKey;
	@DatabaseField
	private boolean isLocalOnly = false;
	@DatabaseField
	private boolean isRepeating = false;
	
	@DatabaseField(defaultValue = "-1", canBeNull = false)
	private int nowPlayingId = -1;
	@DatabaseField(defaultValue = "-1", canBeNull = false)
	private int nowPlayingProgress;
	
	@DatabaseField(defaultValue = "-1", canBeNull = false)
	private int selectedView = -1;
	
	@DatabaseField
	private String savedTracksString;
	
	@ForeignCollectionField(eager = true)
	private Collection<StoredFile> storedFiles = null;
	
	@ForeignCollectionField(eager = true)
	private Collection<StoredList> storedLists = null;
	
	@ForeignCollectionField()
	private Collection<CachedFile> cachedFiles = null;
	
	/**
	 * @return the nowPlayingId
	 */
	public int getNowPlayingId() {
		return nowPlayingId;
	}
	/**
	 * @param nowPlayingId the nowPlayingId to set
	 */
	public void setNowPlayingId(int nowPlayingId) {
		this.nowPlayingId = nowPlayingId;
	}
	
	/**
	 * @return the mLibraryName
	 */
	public String getLibraryName() {
		return libraryName;
	}
	/**
	 * @param mLibraryName the mLibraryName to set
	 */
	public void setLibraryName(String libraryName) {
		this.libraryName = libraryName;
	}
	/**
	 * @return the mAccessCode
	 */
	public String getAccessCode() {
		return accessCode;
	}
	/**
	 * @param mAccessCode the mAccessCode to set
	 */
	public void setAccessCode(String accessCode) {
		this.accessCode = accessCode;
	}

	/**
	 * @return the authKey
	 */
	public String getAuthKey() {
		return authKey;
	}
	/**
	 * @param authKey the authKey to set
	 */
	public void setAuthKey(String authKey) {
		this.authKey = authKey;
	}

	/**
	 * @return the nowPlayingProgress
	 */
	public int getNowPlayingProgress() {
		return nowPlayingProgress;
	}
	/**
	 * @param nowPlayingProgress the nowPlayingProgress to set
	 */
	public void setNowPlayingProgress(int nowPlayingProgress) {
		this.nowPlayingProgress = nowPlayingProgress;
	}
		
	public String getSavedTracksString() {
		return savedTracksString;
	}
	
	public void setSavedTracksString(String savedTracksString) {
		this.savedTracksString = savedTracksString;
	}
		
	public void setSavedTracks(ArrayList<File> files) {
		savedTracksString = Files.serializeFileStringList(files);
	}
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
	/**
	 * @return the isLocalOnly
	 */
	public boolean isLocalOnly() {
		return isLocalOnly;
	}
	/**
	 * @param isLocalOnly the isLocalOnly to set
	 */
	public void setLocalOnly(boolean isLocalOnly) {
		this.isLocalOnly = isLocalOnly;
	}
	/**
	 * @return the selectedView
	 */
	public int getSelectedView() {
		return selectedView;
	}
	/**
	 * @param selectedView the selectedView to set
	 */
	public void setSelectedView(int selectedView) {
		this.selectedView = selectedView;
	}
	/**
	 * @return the isRepeating
	 */
	public boolean isRepeating() {
		return isRepeating;
	}
	/**
	 * @param isRepeating the isRepeating to set
	 */
	public void setRepeating(boolean isRepeating) {
		this.isRepeating = isRepeating;
	}
	
	public Collection<StoredFile> getStoredFiles() {
		if (storedFiles == null)
			storedFiles = new ArrayList<StoredFile>();
		
		return storedFiles;
	}
	
	public Collection<StoredList> getStoredLists() {
		if (storedLists == null)
			storedLists = new ArrayList<StoredList>();
		
		return storedLists;
	}
	/**
	 * @return the cachedFiles
	 */
	public final Collection<CachedFile> getCachedFiles() {
		if (cachedFiles == null)
			cachedFiles = new ArrayList<CachedFile>();
		
		return cachedFiles;
	}
}
