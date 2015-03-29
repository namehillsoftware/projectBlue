package com.lasthopesoftware.bluewater.servers.library;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.servers.library.FileSystem.OnGetFileSystemCompleteListener;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.access.ItemProvider;
import com.lasthopesoftware.bluewater.servers.library.items.list.ClickItemListener;
import com.lasthopesoftware.bluewater.servers.library.items.list.ItemListAdapter;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.ClickPlaylistListener;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.PlaylistListAdapter;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlists;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.access.PlaylistsProvider;
import com.lasthopesoftware.bluewater.shared.listener.LongClickFlipListener;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

import java.util.ArrayList;
import java.util.List;

public class LibraryViewFragment extends Fragment {
	
    private static final String ARG_CATEGORY_POSITION = "category_position";

    public static LibraryViewFragment getPreparedFragment(final int libraryViewId) {
        final LibraryViewFragment returnFragment = new LibraryViewFragment();
        final Bundle args = new Bundle();
        args.putInt(LibraryViewFragment.ARG_CATEGORY_POSITION, libraryViewId);
        returnFragment.setArguments(args);
        return returnFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
    	final Context context = getActivity();
   	
    	final RelativeLayout layout = new RelativeLayout(context);
    	layout.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    	
    	final ProgressBar pbLoading = new ProgressBar(context, null, android.R.attr.progressBarStyleLarge);
    	final RelativeLayout.LayoutParams pbParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    	pbParams.addRule(RelativeLayout.CENTER_IN_PARENT);
    	pbLoading.setLayoutParams(pbParams);
    	layout.addView(pbLoading);
    	
    	FileSystem.Instance.get(context, new OnGetFileSystemCompleteListener() {
			
			@SuppressWarnings("unchecked")
			@Override
			public void onGetFileSystemComplete(FileSystem fileSystem) {

		    	final ISimpleTask.OnCompleteListener<String, Void, ArrayList<IItem>> onGetVisibleViewsCompleteListener = new ISimpleTask.OnCompleteListener<String, Void, ArrayList<IItem>>() {
					
					@Override
					public void onComplete(ISimpleTask<String, Void, ArrayList<IItem>> owner, ArrayList<IItem> result) {
						if (result == null || result.size() == 0) return;

						final int categoryPosition = getArguments().getInt(ARG_CATEGORY_POSITION);
						final IItem category = categoryPosition < result.size() ? result.get(categoryPosition) : result.get(result.size() - 1);
										
						if (category instanceof Playlists)
							layout.addView(BuildPlaylistView(context, pbLoading));
						else if (category instanceof Item)
							layout.addView(BuildStandardItemView(context, (Item)category, pbLoading));
					}
				};
				
				final HandleViewIoException handleViewIoException = new HandleViewIoException(context, new OnConnectionRegainedListener() {
								
					@Override
					public void onConnectionRegained() {
						final OnConnectionRegainedListener _this = this;
						FileSystem.Instance.get(context, new OnGetFileSystemCompleteListener() {
							
							@Override
							public void onGetFileSystemComplete(FileSystem fileSystem) {
								fileSystem.getVisibleViewsAsync(onGetVisibleViewsCompleteListener, new HandleViewIoException(context, _this));
							}
						});
					}
				});
				
				fileSystem.getVisibleViewsAsync(onGetVisibleViewsCompleteListener, handleViewIoException);
			}
             });
    	
        return layout;
    }

	@SuppressWarnings("unchecked")
	private ListView BuildPlaylistView(final Context context, final View loadingView) {
    	
		final ListView listView = new ListView(context);
		listView.setVisibility(View.INVISIBLE);
		final PlaylistsProvider playlistsProvider = new PlaylistsProvider();
		playlistsProvider
			.onComplete(new OnCompleteListener<Void, Void, List<Playlist>>() {
				
				@Override
				public void onComplete(ISimpleTask<Void, Void, List<Playlist>> owner, List<Playlist> result) {
					if (result == null) return;
					
					listView.setOnItemClickListener(new ClickPlaylistListener(context, result));
					listView.setOnItemLongClickListener(new LongClickFlipListener());
		    		listView.setAdapter(new PlaylistListAdapter(context, R.id.tvStandard, result));
		    		loadingView.setVisibility(View.INVISIBLE);
		    		listView.setVisibility(View.VISIBLE);					
				}
			})
			.onError(new HandleViewIoException(context, new OnConnectionRegainedListener() {
					
                @Override
                public void onConnectionRegained() {
                    playlistsProvider.execute();
                }
            }));
		
		playlistsProvider.execute();
		
		return listView;
    }

	@SuppressWarnings("unchecked")
	private ListView BuildStandardItemView(final Context context, final Item category, final View loadingView) {
		final ListView listView = new ListView(context);
    	listView.setVisibility(View.INVISIBLE);
    	
    	final ItemProvider itemProvider = new ItemProvider(category.getKey());
    	
    	itemProvider.onComplete(new OnCompleteListener<Void, Void, List<Item>>() {

			@Override
			public void onComplete(ISimpleTask<Void, Void, List<Item>> owner, List<Item> result) {
				if (result == null) return;

                listView.setOnItemClickListener(new ClickItemListener(context, result instanceof ArrayList ? (ArrayList<Item>)result : new ArrayList<>(result)));
		    	listView.setOnItemLongClickListener(new LongClickFlipListener());
		    	
		    	listView.setAdapter(new ItemListAdapter(context, R.layout.layout_list_item, result));
		    	loadingView.setVisibility(View.INVISIBLE);
	    		listView.setVisibility(View.VISIBLE);
			}
		}).onError(new HandleViewIoException(context, new OnConnectionRegainedListener() {
												
												@Override
												public void onConnectionRegained() {
													itemProvider.execute();
												}
											}));
		
    	itemProvider.execute();
    	
		return listView;
	}
}