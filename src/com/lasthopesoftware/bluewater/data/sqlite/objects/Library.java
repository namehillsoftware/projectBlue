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
	@DatabaseField(columnName = "NOW_PLAYING_ID")
	private int nowPlayingId;
	@DatabaseField(columnName = "NOW_PLAYING_PROGRESS")
	private int nowPlayingProgress;
	@DatabaseField(columnName = "IS_LOCAL_ONLY")
	private boolean isLocalOnly;
	
	@ForeignCollectionField(eager = true)
	private Collection<View> views;
	@ForeignCollectionField(eager = true)
	private Collection<LibraryView> selectedViews;
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
	public Collection<View> getViews() {
		return views;
	}
	/**
	 * @param mViews the mViews to set
	 */
	public void setViews(Collection<View> views) {
		this.views = views;
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
		this.savedTracks = savedTracks;
	}
	
	public void setSavedTracks(ArrayList<JrFile> files) {
		savedTracks = new ArrayList<SavedTrack>(files.size()); 
		for (JrFile file : files) {
			SavedTrack newSavedTrack = new SavedTrack();
			newSavedTrack.setTrackId(file.getKey());
			savedTracks.add(newSavedTrack);			
		}
	}
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
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
	 * @return the selectedViews
	 */
	public Collection<View> getSelectedViews() {
		if (selectedViews == null) selectedViews = new ArrayList<LibraryView>();
		ArrayList<View> returnViews = new ArrayList<View>(selectedViews.size());
		for (LibraryView libraryView : selectedViews)
			returnViews.add(libraryView.getView());
		return returnViews;
	}
	/**
	 * @param selectedViews the selectedViews to set
	 */
	public void setSelectedViews(Collection<View> selectedViews) {
		this.selectedViews = new ArrayList<LibraryView>(selectedViews.size());
		for (View view : selectedViews) {
			LibraryView newLibraryView = new LibraryView();
			newLibraryView.setLibrary(this);
			newLibraryView.setView(view);
			this.selectedViews.add(newLibraryView);
		}
	}
}
