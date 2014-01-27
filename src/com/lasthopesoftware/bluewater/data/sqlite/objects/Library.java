package com.lasthopesoftware.bluewater.data.sqlite.objects;

import java.util.ArrayList;
import java.util.Collection;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import com.lasthopesoftware.bluewater.data.service.objects.JrFile;

@DatabaseTable(tableName = "LIBRARIES")
public class Library {
	
	@DatabaseField(generatedId = true)
	private int id;
	@DatabaseField(canBeNull = false, columnName = "LIBRARY_NAME", columnDefinition = "VARCHAR(50)")
	private String libraryName;
	@DatabaseField(canBeNull = false, columnName = "ACCESS_CODE", columnDefinition = "VARCHAR(30)")
	private String accessCode;
	@DatabaseField(columnName = "AUTH_KEY", columnDefinition = "VARCHAR(100)")
	private String authKey;
	@DatabaseField(columnName = "NOW_PLAYING_ID", defaultValue = "-1")
	private int nowPlayingId;
	@DatabaseField(columnName = "NOW_PLAYING_PROGRESS", defaultValue = "-1")
	private int nowPlayingProgress;
	@DatabaseField(columnName = "IS_LOCAL_ONLY")
	private boolean isLocalOnly = false;
	
	@ForeignCollectionField(eager = true)
	private Collection<SelectedView> selectedViews;
	@ForeignCollectionField(eager = true)
	private Collection<SavedTrack> savedTracks;
	
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
	 * @return the mViews
	 */
	public Collection<SelectedView> getSelectedViews() {
		if (selectedViews == null) selectedViews = new ArrayList<SelectedView>();
		return selectedViews;
	}
	/**
	 * @param mViews the mViews to set
	 */
	public void setSelectedViews(Collection<SelectedView> views) {
		for (SelectedView view : views)
			view.setLibrary(this);
		
		this.selectedViews = views;
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
	/**
	 * @return the savedTracks
	 */
	public Collection<SavedTrack> getSavedTracks() {
		if (savedTracks == null) savedTracks = new ArrayList<SavedTrack>();
		return savedTracks;
	}
	
	public String getSavedTracksString() {
		String trackStringList = "2;" + String.valueOf(savedTracks.size()) + ";-1;";
		for (SavedTrack track : savedTracks)
			trackStringList += String.valueOf(track.getTrackId()) + ";";
		
		return trackStringList;
	}
	/**
	 * @param savedTracks the savedTracks to set
	 */
	public void setSavedTracks(Collection<SavedTrack> savedTracks) {
		for (SavedTrack track : savedTracks)
			track.setLibrary(this);
		
		this.savedTracks = savedTracks;
	}
	
	public void setSavedTracks(ArrayList<JrFile> files) {
		ArrayList<SavedTrack> newSavedTracks = new ArrayList<SavedTrack>(files.size()); 
		for (JrFile file : files) {
			SavedTrack newSavedTrack = new SavedTrack();
			newSavedTrack.setTrackId(file.getKey());
			newSavedTracks.add(newSavedTrack);			
		}
		setSavedTracks(newSavedTracks);
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
}
