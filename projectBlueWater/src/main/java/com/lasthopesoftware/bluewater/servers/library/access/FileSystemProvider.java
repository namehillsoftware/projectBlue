package com.lasthopesoftware.bluewater.servers.library.access;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.FileSystem;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.access.AbstractCollectionProvider;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 3/17/15.
 */
public class FileSystemProvider extends AbstractCollectionProvider<FileSystem, Item> {

    private static List<Item> mCachedFileSystemItems;
    private static Integer mRevision;

    public FileSystemProvider(FileSystem item) {
        super(item);
    }

    public FileSystemProvider(HttpURLConnection connection, FileSystem item) {
        super(connection, item);
    }

    @Override
    protected SimpleTask<Void, Void, List<Item>> buildTask(final FileSystem fileSystem) {
        final SimpleTask<Void, Void, List<Item>> fileSystemTask = new SimpleTask<>(new ISimpleTask.OnExecuteListener<Void, Void, List<Item>>() {
            @Override
            public List<Item> onExecute(ISimpleTask<Void, Void, List<Item>> owner, Void... params) throws Exception {
                if (mRevision.equals(RevisionChecker.getRevision()) && mCachedFileSystemItems != null) return mCachedFileSystemItems;

                if (owner.isCancelled()) return new ArrayList<>();
                final HttpURLConnection conn = mConnection == null ? ConnectionProvider.getConnection(fileSystem.getSubItemParams()) : mConnection;
                try {
                    final InputStream is = conn.getInputStream();
                    try {
                        mCachedFileSystemItems = FilesystemResponse.GetItems(is);

                        return mCachedFileSystemItems;
                    } finally {
                        is.close();
                    }

                } finally {
                    if (mConnection == null) conn.disconnect();
                }
            }
        });

        fileSystemTask.addOnErrorListener(new ISimpleTask.OnErrorListener<Void, Void, List<Item>>() {
            @Override
            public boolean onError(ISimpleTask<Void, Void, List<Item>> owner, boolean isHandled, Exception innerException) {
                setException(innerException);
                return false;
            }
        });

        if (mOnGetItemsComplete != null)
            fileSystemTask.addOnCompleteListener(mOnGetItemsComplete);

        if (mOnGetItemsError != null)
            fileSystemTask.addOnErrorListener(mOnGetItemsError);

        return fileSystemTask;
    }
}
