package com.lasthopesoftware.bluewater.client.library.repository;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Keep;
import android.util.Base64;
import com.lasthopesoftware.bluewater.repository.IEntityCreator;
import com.lasthopesoftware.bluewater.repository.IEntityUpdater;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Keep
public class Library implements IEntityCreator, IEntityUpdater {

	public static final String tableName = "LIBRARIES";
	public static final String libraryNameColumn = "libraryName";
	public static final String accessCodeColumn = "accessCode";
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
	public static final String userNameColumn = "userName";
	public static final String passwordColumn = "password";

	private int id = -1;
	
	// Remote connection fields
	private String libraryName;
	private String accessCode;
	private String userName;
	private String password;
	private boolean isLocalOnly = false;
	private boolean isRepeating = false;
	private int nowPlayingId = -1;
	private long nowPlayingProgress = -1;
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
	public Library setNowPlayingId(int nowPlayingId) {
		this.nowPlayingId = nowPlayingId;
		return this;
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
	public Library setLibraryName(String libraryName) {
		this.libraryName = libraryName;
		return this;
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
	public Library setAccessCode(String accessCode) {
		this.accessCode = accessCode;
		return this;
	}

	/**
	 * @return the nowPlayingProgress
	 */
	public long getNowPlayingProgress() {
		return nowPlayingProgress;
	}
	/**
	 * @param nowPlayingProgress the nowPlayingProgress to set
	 */
	public Library setNowPlayingProgress(long nowPlayingProgress) {
		this.nowPlayingProgress = nowPlayingProgress;
		return this;
	}
		
	public String getSavedTracksString() {
		return savedTracksString;
	}
	
	public Library setSavedTracksString(String savedTracksString) {
		this.savedTracksString = savedTracksString;
		return this;
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
	public Library setLocalOnly(boolean isLocalOnly) {
		this.isLocalOnly = isLocalOnly;
		return this;
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
	public Library setSelectedView(int selectedView) {
		this.selectedView = selectedView;
		return this;
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
	public Library setRepeating(boolean isRepeating) {
		this.isRepeating = isRepeating;
		return this;
	}

	public String getCustomSyncedFilesPath() {
		return customSyncedFilesPath;
	}

	public Library setCustomSyncedFilesPath(String customSyncedFilesPath) {
		this.customSyncedFilesPath = customSyncedFilesPath;
		return this;
	}

	public SyncedFileLocation getSyncedFileLocation() {
		return syncedFileLocation;
	}

	public Library setSyncedFileLocation(SyncedFileLocation syncedFileLocation) {
		this.syncedFileLocation = syncedFileLocation;
		return this;
	}

	public boolean isUsingExistingFiles() {
		return isUsingExistingFiles;
	}

	public Library setIsUsingExistingFiles(boolean isUsingExistingFiles) {
		this.isUsingExistingFiles = isUsingExistingFiles;
		return this;
	}

	public boolean isSyncLocalConnectionsOnly() {
		return isSyncLocalConnectionsOnly;
	}

	public Library setIsSyncLocalConnectionsOnly(boolean isSyncLocalConnections) {
		this.isSyncLocalConnectionsOnly = isSyncLocalConnections;
		return this;
	}

	public ViewType getSelectedViewType() {
		return selectedViewType;
	}

	public Library setSelectedViewType(ViewType selectedViewType) {
		this.selectedViewType = selectedViewType;
		return this;
	}

	public Library setId(int id) {
		this.id = id;
		return this;
	}

	public String getUserName() {
		return userName;
	}

	public Library setUserName(String userName) {
		this.userName = userName;
		return this;
	}

	public String getPassword() {
		return password;
	}

	public Library setPassword(String password) {
		this.password = password;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Library library = (Library) o;
		return id == library.id;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE `LIBRARIES` (`accessCode` VARCHAR(30) , `userName` VARCHAR , `password` VARCHAR , `customSyncedFilesPath` VARCHAR , `id` INTEGER PRIMARY KEY AUTOINCREMENT , `isLocalOnly` SMALLINT , `isRepeating` SMALLINT , `isSyncLocalConnectionsOnly` SMALLINT , `isUsingExistingFiles` SMALLINT , `libraryName` VARCHAR(50) , `nowPlayingId` INTEGER DEFAULT -1 NOT NULL , `nowPlayingProgress` INTEGER DEFAULT -1 NOT NULL , `savedTracksString` VARCHAR , `selectedView` INTEGER DEFAULT -1 NOT NULL , `selectedViewType` VARCHAR , `syncedFileLocation` VARCHAR )");
	}

	@Override
	public void onUpdate(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 5) {
			db.execSQL("ALTER TABLE `LIBRARIES` add column `customSyncedFilesPath` VARCHAR;");
			db.execSQL("ALTER TABLE `LIBRARIES` add column `syncedFileLocation` VARCHAR DEFAULT 'INTERNAL';");
			db.execSQL("ALTER TABLE `LIBRARIES` add column `isUsingExistingFiles` BOOLEAN DEFAULT 0;");
			db.execSQL("ALTER TABLE `LIBRARIES` add column `isSyncLocalConnectionsOnly` BOOLEAN DEFAULT 0;");
			db.execSQL("ALTER TABLE `LIBRARIES` add column `selectedViewType` VARCHAR;");
		}

		if (oldVersion < 7) {
			db.execSQL("ALTER TABLE `LIBRARIES` add column `userName` VARCHAR;");
			db.execSQL("ALTER TABLE `LIBRARIES` add column `password` VARCHAR;");
			try (final Cursor cursor = db.rawQuery("SELECT ID, authKey FROM `LIBRARIES` WHERE `authKey` IS NOT NULL AND `authKey` <> ''", new String[0])) {
				if (cursor.moveToFirst() && cursor.getCount() > 0) {
					do {
						final int libraryId = cursor.getInt(0);
						final String authKey = cursor.getString(1);
						final String decodedAuthKey = new String(Base64.decode(authKey, Base64.DEFAULT));
						final String[] userCredentials = decodedAuthKey.split(":");

						if (userCredentials.length > 1) {
							db.execSQL(
								"UPDATE `" + Library.tableName + "` " +
									" SET `" + userNameColumn + "` = ?, " +
									" `" + passwordColumn + "` = ? " +
									" WHERE `id` = ?",
								new Object[] {
									userCredentials[0],
									userCredentials[1],
									libraryId
								});

							continue;
						}

						if (userCredentials.length > 0) {
							db.execSQL(
								"UPDATE `" + Library.tableName + "` " +
									" SET `" + userNameColumn + "` = ? " +
									" WHERE `id` = ?",
								new Object[] {
									userCredentials[0],
									libraryId
								});
						}
					} while (cursor.moveToNext());
				}
			}
		}
	}

	public enum SyncedFileLocation {
		EXTERNAL,
		INTERNAL,
		CUSTOM;

		public static final Set<SyncedFileLocation> ExternalDiskAccessSyncLocations = Collections.unmodifiableSet(
				new HashSet<>(
						Arrays.asList(SyncedFileLocation.EXTERNAL,
							SyncedFileLocation.CUSTOM)));
	}

	public enum ViewType {
		StandardServerView,
		PlaylistView,
		DownloadView;

		public static final Set<ViewType> serverViewTypes = Collections.unmodifiableSet(
				new HashSet<>(Arrays.asList(Library.ViewType.StandardServerView, Library.ViewType.PlaylistView)));
	}
}
