package com.lasthopesoftware.bluewater.servers.library.items.media.files.storage;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.lasthopesoftware.bluewater.disk.sqlite.access.DatabaseHandler;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.StoredFile;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.StoredList;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFilesContainer;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.storage.downloads.provider.FileDownloadProvider;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by david on 7/5/15.
 */
public class SyncListManager {

    private static final Logger mLogger = LoggerFactory.getLogger(SyncListManager.class);

    private final Context mContext;

    private static final ExecutorService mSyncExecutor = Executors.newSingleThreadExecutor();

    public SyncListManager(Context context) {
        mContext = context;
    }

    public void queueSync() {
        mSyncExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final DatabaseHandler dbHandler = new DatabaseHandler(mContext);
                try {
                    final Dao<StoredList, Integer> storedListAccess = dbHandler.getAccessObject(StoredList.class);
                    final List<StoredList> listsToSync = storedListAccess.queryForAll();

                    final FileDownloadProvider fileDownloadProvider = new FileDownloadProvider(mContext);
                    final Library library = LibrarySession.GetLibrary(mContext);
                    final Dao<StoredFile, Integer> storedFileAccess = dbHandler.getAccessObject(StoredFile.class);

                    final Set<Integer> allSyncedFileKeys = new HashSet<>();
                    for (StoredList listToSync : listsToSync) {
                        final int serviceId = listToSync.getServiceId();
                        final IFilesContainer filesContainer = listToSync.getType() == StoredList.ListType.ITEM ? new Item(serviceId) : new Playlist(serviceId);
                        final ArrayList<IFile> files = filesContainer.getFiles().getFiles();
                        for (IFile file : files)
                            allSyncedFileKeys.add(file.getKey());

                        syncFiles(storedFileAccess, fileDownloadProvider, library, files);
                    }

                    final List<StoredFile> allStoredFiles = getAllStoredFiles(storedFileAccess, library);
                    for (StoredFile storedFile : allStoredFiles) {
                        if (!allSyncedFileKeys.contains(storedFile.getServiceId()))
                            deleteStoredFile(storedFileAccess, storedFile);
                    }
                } catch (SQLException e) {
                    mLogger.error("Error accessing the stored list access", e);
                } finally {
                    dbHandler.close();
                }
            }
        });
    }

    public void syncItem(Item item) {
        markItemForSync(item, StoredList.ListType.ITEM);
        queueSync();
    }

    public void syncPlaylist(Playlist playlist) {
        markItemForSync(playlist, StoredList.ListType.PLAYLIST);
        queueSync();
    }

    public void isItemMarkedForSync(final IItem item, ISimpleTask.OnCompleteListener<Void, Void, Boolean> isItemSyncedResult) {
        final SimpleTask<Void, Void, Boolean> isItemSyncedTask = new SimpleTask<>(new ISimpleTask.OnExecuteListener<Void, Void, Boolean>() {
            @Override
            public Boolean onExecute(ISimpleTask<Void, Void, Boolean> owner, Void... params) throws Exception {
                final DatabaseHandler dbHandler = new DatabaseHandler(mContext);
                try {
                    final Dao<StoredList, Integer> storedListAccess = dbHandler.getAccessObject(StoredList.class);
                    return isItemMarkedForSync(storedListAccess, LibrarySession.GetLibrary(mContext), item);
                } finally {
                    dbHandler.close();
                }
            }
        });

        if (isItemSyncedResult != null)
            isItemSyncedTask.addOnCompleteListener(isItemSyncedResult);

        isItemSyncedTask.execute(mSyncExecutor);
    }

    private void markItemForSync(final IItem item, final StoredList.ListType listType) {
        mSyncExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final DatabaseHandler dbHandler = new DatabaseHandler(mContext);
                try {
                    final Dao<StoredList, Integer> storedListAccess = dbHandler.getAccessObject(StoredList.class);

                    final Library library = LibrarySession.GetLibrary(mContext);
                    if (isItemMarkedForSync(storedListAccess, library, item)) return;

                    final StoredList storedList = new StoredList();
                    storedList.setLibrary(library);
                    storedList.setServiceId(item.getKey());
                    storedList.setType(listType);

                    try {
                        storedListAccess.create(storedList);
                    } catch (SQLException e) {
                        mLogger.error("Error while creating new stored list", e);
                    }
                } catch (SQLException e) {
                    mLogger.error("Error getting acces to the stored list table", e);
                } finally {
                    dbHandler.close();
                }
            }
        });
    }

    private static boolean isItemMarkedForSync(Dao<StoredList, Integer> storedListAccess, Library library, IItem item) {
        try {
            final PreparedQuery<StoredList> storedListPreparedQuery =
                    storedListAccess
                            .queryBuilder()
                            .where()
                            .eq(StoredFile.serviceIdColumnName, item.getKey())
                            .and()
                            .eq(StoredFile.libraryIdColumnName, library.getId())
                            .prepare();
            return storedListAccess.queryForFirst(storedListPreparedQuery) != null;
        } catch (SQLException e) {
            mLogger.error("Error while checking whether stored list exists.", e);
        }

        return false;
    }

    private static void syncFiles(Dao<StoredFile, Integer> storedFilesAccess, FileDownloadProvider fileDownloadProvider, Library library, List<IFile> files) {
        try {
            for (IFile file : files) {
                final PreparedQuery<StoredFile> storedFilePreparedQuery =
                        storedFilesAccess
                            .queryBuilder()
                            .where()
                                .eq(StoredFile.serviceIdColumnName, file.getKey())
                            .and()
                                .eq(StoredFile.libraryIdColumnName, library.getId())
                            .prepare();

                StoredFile storedFile = storedFilesAccess.queryForFirst(storedFilePreparedQuery);
                if (storedFile == null) {
                    storedFile = new StoredFile();
                    storedFile.setServiceId(file.getKey());
                    storedFile.setLibrary(library);
                    storedFile.setIsOwner(true);

                    try {
                        storedFile.setPath(FilenameUtils.concat(FilenameUtils.concat(library.getSyncedFilesPath(), String.valueOf(library.getId())), file.getProperty(FilePropertiesProvider.FILENAME)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (!storedFile.isDownloadComplete() || storedFile.getDownloadId() > -1) continue;

                storedFile.setDownloadId(fileDownloadProvider.downloadFile(file, storedFile.getPath()));
                storedFilesAccess.createOrUpdate(storedFile);
            }
        } catch (SQLException e) {
            mLogger.error("There was an updating the stored file.", e);
        }
    }

    private static List<StoredFile> getAllStoredFiles(Dao<StoredFile, Integer> storedFilesAccess, Library library) {
        try {
            return storedFilesAccess.queryForEq(StoredFile.libraryIdColumnName, library.getId());
        } catch (SQLException e) {
            mLogger.error("There was an error pulling all stored files", e);
            return new ArrayList<>();
        }
    }

    private static void deleteStoredFile(Dao<StoredFile, Integer> storedFilesAccess, StoredFile storedFile) {
        try {
            storedFilesAccess.delete(storedFile);
            final File file = new File(storedFile.getPath());
            file.delete();
        } catch (SQLException e) {
            mLogger.error("There was an deleting the stored file.", e);
        }
    }
}
