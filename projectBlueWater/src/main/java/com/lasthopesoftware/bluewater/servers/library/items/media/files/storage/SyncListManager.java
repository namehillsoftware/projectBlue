package com.lasthopesoftware.bluewater.servers.library.items.media.files.storage;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.lasthopesoftware.bluewater.disk.sqlite.access.DatabaseHandler;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.StoredFile;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.StoredList;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFilesContainer;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.storage.downloads.provider.FileDownloadProvider;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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

    public void syncAll() {
        SimpleTask.executeNew(mSyncExecutor, new ISimpleTask.OnExecuteListener<Void, Void, Void>() {
            @Override
            public Void onExecute(ISimpleTask<Void, Void, Void> owner, Void... params) throws Exception {
                final DatabaseHandler dbHandler = new DatabaseHandler(mContext);
                try {
                    final Dao<StoredList, Integer> storedListAccess = dbHandler.getAccessObject(StoredList.class);
                    final List<StoredList> listsToSync = storedListAccess.queryForAll();

                    final FileDownloadProvider fileDownloadProvider = new FileDownloadProvider(mContext);
                    final Library library = LibrarySession.GetLibrary(mContext);
                    final Dao<StoredFile, Integer> storedFileAccess = dbHandler.getAccessObject(StoredFile.class);

                    final ArrayList<IFile> allFilesToSync = new ArrayList<>();
                    for (StoredList listToSync : listsToSync) {
                        final int serviceId = listToSync.getServiceId();
                        final IFilesContainer filesContainer = listToSync.getType() == StoredList.ListType.ITEM ? new Item(serviceId) : new Playlist(serviceId);
                        final ArrayList<IFile> files = filesContainer.getFiles().getFiles();
                        allFilesToSync.addAll(files);
                        syncFiles(storedFileAccess, fileDownloadProvider, library, files);
                    }
                } finally {
                    dbHandler.close();
                }

                return null;
            }
        });
    }

    public void syncItem(Item item) {
        markItemForSync(item, StoredList.ListType.ITEM);
    }

    public void syncPlaylist(Playlist playlist) {
        markItemForSync(playlist, StoredList.ListType.PLAYLIST);
    }

    public void isItemChildrenSynced(final IItem item, ISimpleTask.OnCompleteListener<Void, Void, Boolean> isItemSyncedResult) {
        final SimpleTask<Void, Void, Boolean> isItemSyncedTask = new SimpleTask<>(new ISimpleTask.OnExecuteListener<Void, Void, Boolean>() {
            @Override
            public Boolean onExecute(ISimpleTask<Void, Void, Boolean> owner, Void... params) throws Exception {
                final DatabaseHandler dbHandler = new DatabaseHandler(mContext);
                try {
                    final Dao<StoredList, Integer> storedListAccess = dbHandler.getAccessObject(StoredList.class);
                    return isItemChildrenSynced(storedListAccess, item);
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
        SimpleTask.executeNew(mSyncExecutor, new ISimpleTask.OnExecuteListener<Void, Void, Void>() {
            @Override
            public Void onExecute(ISimpleTask<Void, Void, Void> owner, Void... params) throws Exception {
                final DatabaseHandler dbHandler = new DatabaseHandler(mContext);
                try {
                    final Dao<StoredList, Integer> storedListAccess = dbHandler.getAccessObject(StoredList.class);

                    if (isItemChildrenSynced(storedListAccess, item)) return null;

                    final StoredList storedList = new StoredList();
                    storedList.setLibrary(LibrarySession.GetLibrary(mContext));
                    storedList.setServiceId(item.getKey());
                    storedList.setType(listType);

                    try {
                        storedListAccess.create(storedList);
                    } catch (SQLException e) {
                        mLogger.error("Error while creating new stored list", e);
                    }
                } finally {
                    dbHandler.close();
                }
                return null;
            }
        });
    }

    private static boolean isItemChildrenSynced(Dao<StoredList, Integer> storedListAccess, IItem item) {
        try {
            return !storedListAccess.queryForEq(StoredList.serviceIdColumnName, item.getKey()).isEmpty();
        } catch (SQLException e) {
            mLogger.error("Error while checking whether stored list exists.", e);
        }

        return false;
    }

    private static void syncFiles(Dao<StoredFile, Integer> storedFilesAccess, FileDownloadProvider fileDownloadProvider, Library library, List<IFile> files) {
        try {
            for (IFile file : files) {
                StoredFile storedFile = new StoredFile();
                storedFile.setServiceId(file.getKey());
                storedFile.setLibrary(library);

                final List<StoredFile> storedFiles = storedFilesAccess.queryForEq(StoredFile.serviceIdColumnName, file.getKey());
                if (!storedFiles.isEmpty())
                    storedFile = storedFiles.get(0);

                if (storedFile.isDownloadComplete() || storedFile.getDownloadId() > -1) continue;

                storedFile.setDownloadId(fileDownloadProvider.downloadFile(file));
                storedFilesAccess.createOrUpdate(storedFile);
            }
        } catch (SQLException e) {
            mLogger.error("There was an updating the stored file.", e);
        }
    }
}
