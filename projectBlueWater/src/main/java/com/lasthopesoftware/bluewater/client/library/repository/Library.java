package com.lasthopesoftware.bluewater.client.library.repository;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import com.lasthopesoftware.bluewater.repository.IEntityCreator;
import com.lasthopesoftware.bluewater.repository.IEntityUpdater;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Library implements IEntityCreator, IEntityUpdater {

	public static final String tableName = "LIBRARIES";
	public static final String libraryNameColumn = "libraryName";
	public static final String accessCodeColumn = "accessCode";
	public static final String authKeyColumn = "authKey";
	public static final String isLocalOnlyColumn = "isLocalOnly";
	public static final String isRepeatingColumn = "isRepeating";
	public static final String nowPlayingIdColumn = "nowPlayingId";
	public static final String nowPlayingProgressColumn = "nowPlayingProgress";
	public static final String selectedViewTypeColumn = "selectedViewType";
	public static final String selectedViewColumn = "selectedView";
	public static final String savedTracksStringColumn = "savedTracksString";
	public static final String customSyncedFilesPathColumn = "customSyncedFilesPath";
	public static final String syncedFileLocationColumn = "syncedFileLocation";
	public static final String isUsingExistingFilesColumn = "isUsingExistingFiles";
	public static final String isSyncLocalConnectionsOnlyColumn = "isSyncLocalConnectionsOnly";

	private int id = -1;
	
	// Remote connection fields
	private String libraryName;
	private String accessCode;
	private String authKey;
	private boolean isLocalOnly = false;
	private boolean isRepeating = false;
	private int nowPlayingId = -1;
	private int nowPlayingProgress = -1;
	private ViewType selectedViewType;
	private int selectedView = -1;
	private String savedTracksString;
	private String customSyncedFilesPath;
	private SyncedFileLocation syncedFileLocation;
	private boolean isUsingExistingFiles;
	private boolean isSyncLocalConnectionsOnly;

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
	 * @param libraryName the mLibraryName to set
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
	 * @param accessCode the mAccessCode to set
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

	public String getCustomSyncedFilesPath() {
		return customSyncedFilesPath;
	}

	public void setCustomSyncedFilesPath(String customSyncedFilesPath) {
		this.customSyncedFilesPath = customSyncedFilesPath;
	}

	public File getSyncDir(Context context) {
		return syncedFileLocation != SyncedFileLocation.CUSTOM ? buildSyncDir(context, syncedFileLocation) : new File(customSyncedFilesPath);
	}

	private File buildSyncDir(Context context, SyncedFileLocation syncedFileLocation) {
		File parentSyncDir = null;
		switch (syncedFileLocation) {
			case EXTERNAL:
				parentSyncDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
				break;
			case INTERNAL:
				parentSyncDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), id > -1 ? String.valueOf(id) : "");
				break;
		}

		return parentSyncDir;
	}

	public SyncedFileLocation getSyncedFileLocation() {
		return syncedFileLocation;
	}

	public void setSyncedFileLocation(SyncedFileLocation syncedFileLocation) {
		this.syncedFileLocation = syncedFileLocation;
	}

	public boolean isUsingExistingFiles() {
		return isUsingExistingFiles;
	}

	public void setIsUsingExistingFiles(boolean isUsingExistingFiles) {
		this.isUsingExistingFiles = isUsingExistingFiles;
	}

	public boolean isSyncLocalConnectionsOnly() {
		return isSyncLocalConnectionsOnly;
	}

	public void setIsSyncLocalConnectionsOnly(boolean isSyncLocalConnections) {
		this.isSyncLocalConnectionsOnly = isSyncLocalConnections;
	}

	public ViewType getSelectedViewType() {
		return selectedViewType;
	}

	public void setSelectedViewType(ViewType selectedViewType) {
		this.selectedViewType = selectedViewType;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE `LIBRARIES` (`accessCode` VARCHAR(30) , `authKey` VARCHAR(100) , `customSyncedFilesPath` VARCHAR , `id` INTEGER PRIMARY KEY AUTOINCREMENT , `isLocalOnly` SMALLINT , `isRepeating` SMALLINT , `isSyncLocalConnectionsOnly` SMALLINT , `isUsingExistingFiles` SMALLINT , `libraryName` VARCHAR(50) , `nowPlayingId` INTEGER DEFAULT -1 NOT NULL , `nowPlayingProgress` INTEGER DEFAULT -1 NOT NULL , `savedTracksString` VARCHAR , `selectedView` INTEGER DEFAULT -1 NOT NULL , `selectedViewType` VARCHAR , `syncedFileLocation` VARCHAR )");
	}

	@Override
	public void onUpdate(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion >= 5) return;

		db.execSQL("ALTER TABLE `LIBRARIES` add column `customSyncedFilesPath` VARCHAR;");
		db.execSQL("ALTER TABLE `LIBRARIES` add column `syncedFileLocation` VARCHAR DEFAULT 'INTERNAL';");
		db.execSQL("ALTER TABLE `LIBRARIES` add column `isUsingExistingFiles` BOOLEAN DEFAULT 0;");
		db.execSQL("ALTER TABLE `LIBRARIES` add column `isSyncLocalConnectionsOnly` BOOLEAN DEFAULT 0;");
		db.execSQL("ALTER TABLE `LIBRARIES` add column `selectedViewType` VARCHAR;");
	}

	public enum SyncedFileLocation {
		EXTERNAL,
		INTERNAL,
		CUSTOM;

		public static final Set<SyncedFileLocation> ExternalDiskAccessSyncLocations = Collections.unmodifiableSet(
				new HashSet<>(
						Arrays.asList(new SyncedFileLocation[] {
								SyncedFileLocation.EXTERNAL,
								SyncedFileLocation.CUSTOM })));
	}

	public enum ViewType {
		StandardServerView,
		PlaylistView,
		DownloadView;

		public static final Set<ViewType> serverViewTypes = Collections.unmodifiableSet(
				new HashSet<>(Arrays.asList(Library.ViewType.StandardServerView, Library.ViewType.PlaylistView)));
	}
}
