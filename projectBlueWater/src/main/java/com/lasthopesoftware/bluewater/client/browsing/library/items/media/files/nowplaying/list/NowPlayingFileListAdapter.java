package com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.nowplaying.list;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.browsing.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.list.AbstractFileListAdapter;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.nowplaying.menu.NowPlayingFileListItemMenuBuilder;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.nowplaying.storage.INowPlayingRepository;
import com.lasthopesoftware.bluewater.client.browsing.library.items.menu.handlers.ViewChangedHandler;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.List;

public class NowPlayingFileListAdapter extends AbstractFileListAdapter implements OneParameterAction<Integer> {

    private final NowPlayingFileListItemMenuBuilder nowPlayingFileListItemMenuBuilder;

	public NowPlayingFileListAdapter(Context context, int resource, IItemListMenuChangeHandler itemListMenuChangeHandler, List<ServiceFile> serviceFiles, INowPlayingRepository nowPlayingRepository) {
		super(context, resource, serviceFiles);

        final ViewChangedHandler viewChangedHandler = new ViewChangedHandler();
        viewChangedHandler.setOnAllMenusHidden(itemListMenuChangeHandler);
        viewChangedHandler.setOnAnyMenuShown(itemListMenuChangeHandler);
        viewChangedHandler.setOnViewChangedListener(itemListMenuChangeHandler);

        nowPlayingFileListItemMenuBuilder = new NowPlayingFileListItemMenuBuilder(nowPlayingRepository);
        nowPlayingFileListItemMenuBuilder.setOnViewChangedListener(viewChangedHandler);
        nowPlayingFileListItemMenuBuilder.setOnPlaylistFileRemovedListener(this);
	}

    @NonNull
    @Override
    public final View getView(final int position, View convertView, @NonNull final ViewGroup parent) {
        return nowPlayingFileListItemMenuBuilder.getView(position, getItem(position), convertView, parent);
    }

    @Override
    public void runWith(Integer position) {
        remove(getItem(position));
    }
}
