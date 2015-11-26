package com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.menu.listeners;

import android.os.AsyncTask;
import android.view.View;

import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.Files;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackController;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.servers.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.AbstractMenuClickHandler;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.servers.library.repository.LibrarySession;
import com.lasthopesoftware.runnables.IOneParameterRunnable;
import com.lasthopesoftware.threading.ISimpleTask;

import java.util.List;

/**
 * Created by david on 11/8/15.
 */
public class RemovePlaylistFileClickListener extends AbstractMenuClickHandler {
    private final int position;
    private final IOneParameterRunnable<Integer> onPlaylistFileRemoved;

    // TODO Add event and remove interdepency on NowPlayingFileListAdapter adapter
    public RemovePlaylistFileClickListener(NotifyOnFlipViewAnimator parent, final int position, final IOneParameterRunnable<Integer> onPlaylistFileRemoved) {
        super(parent);
        this.position = position;
        this.onPlaylistFileRemoved = onPlaylistFileRemoved;
    }

    @Override
    public void onClick(final View view) {
        LibrarySession.GetActiveLibrary(view.getContext(), new ISimpleTask.OnCompleteListener<Integer, Void, Library>() {

            @Override
            public void onComplete(ISimpleTask<Integer, Void, Library> owner, final Library library) {
                if (library == null) return;

                // It could take quite a while to split string and put it back together, so let's do it
                // in a background task
                (new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... params) {
                        final PlaybackController playbackController = PlaybackService.getPlaylistController();
                        if (playbackController != null) {
                            playbackController.removeFile(position);
                            return playbackController.getPlaylistString();
                        }

                        final List<IFile> savedTracks = Files.parseFileStringList(SessionConnection.getSessionConnectionProvider(), library.getSavedTracksString());
                        savedTracks.remove(position);
                        return Files.serializeFileStringList(savedTracks);
                    }

                    @Override
                    protected void onPostExecute(String s) {
                        super.onPostExecute(s);

                        library.setSavedTracksString(s);

                        LibrarySession.SaveLibrary(view.getContext(), library, new ISimpleTask.OnCompleteListener<Void, Void, Library>() {

                            @Override
                            public void onComplete(ISimpleTask<Void, Void, Library> owner, Library result) {
                                if (onPlaylistFileRemoved != null)
                                    onPlaylistFileRemoved.run(position);
                            }
                        });
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

        });

        super.onClick(view);
    }
}
