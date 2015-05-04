package com.lasthopesoftware.bluewater.servers.library;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.PointTarget;
import com.lasthopesoftware.bluewater.ApplicationConstants;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.servers.library.FileSystem.OnGetFileSystemCompleteListener;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.access.ItemProvider;
import com.lasthopesoftware.bluewater.servers.library.items.list.ClickItemListener;
import com.lasthopesoftware.bluewater.servers.library.items.list.ItemListAdapter;
import com.lasthopesoftware.bluewater.servers.library.items.menu.LongClickViewFlipListener;
import com.lasthopesoftware.bluewater.servers.library.items.menu.OnViewFlippedListener;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.ClickPlaylistListener;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlists;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.access.PlaylistsProvider;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

import java.util.ArrayList;
import java.util.List;

public class LibraryViewFragment extends Fragment {

    private static final String ARG_CATEGORY_POSITION = "category_position";
    private static final String PREFS_KEY = "com.lasthopesoftware.bluewater.servers.library.LibraryViewFragment.TUTORIAL_SHOWN";

    private static boolean wasTutorialShown;

    private OnViewFlippedListener mOnViewFlippedListener;

    public static LibraryViewFragment getPreparedFragment(final int libraryViewId) {
        final LibraryViewFragment returnFragment = new LibraryViewFragment();
        final Bundle args = new Bundle();
        args.putInt(LibraryViewFragment.ARG_CATEGORY_POSITION, libraryViewId);
        returnFragment.setArguments(args);
        return returnFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
    	final Activity activity = getActivity();

    	final RelativeLayout layout = new RelativeLayout(activity);
    	layout.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

    	final ProgressBar pbLoading = new ProgressBar(activity, null, android.R.attr.progressBarStyleLarge);
    	final RelativeLayout.LayoutParams pbParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    	pbParams.addRule(RelativeLayout.CENTER_IN_PARENT);
    	pbLoading.setLayoutParams(pbParams);
    	layout.addView(pbLoading);

    	FileSystem.Instance.get(activity, new OnGetFileSystemCompleteListener() {

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
                            layout.addView(BuildPlaylistView(activity, container, categoryPosition, pbLoading));
                        else if (category instanceof Item)
                            layout.addView(BuildStandardItemView(activity, container, categoryPosition, (Item) category, pbLoading));
					}
				};

				final HandleViewIoException handleViewIoException = new HandleViewIoException(activity, new OnConnectionRegainedListener() {

					@Override
					public void onConnectionRegained() {
						final OnConnectionRegainedListener _this = this;
						FileSystem.Instance.get(activity, new OnGetFileSystemCompleteListener() {

							@Override
							public void onGetFileSystemComplete(FileSystem fileSystem) {
								fileSystem.getVisibleViewsAsync(onGetVisibleViewsCompleteListener, new HandleViewIoException(activity, _this));
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
	private ListView BuildPlaylistView(final Activity activity, final ViewGroup container, final int position, final View loadingView) {

		final ListView listView = new ListView(activity);
		listView.setVisibility(View.INVISIBLE);
		final PlaylistsProvider playlistsProvider = new PlaylistsProvider();
		playlistsProvider
			.onComplete(new OnCompleteListener<Void, Void, List<Playlist>>() {

				@Override
				public void onComplete(ISimpleTask<Void, Void, List<Playlist>> owner, List<Playlist> result) {
					if (result == null) return;

					listView.setOnItemClickListener(new ClickPlaylistListener(activity, result));
					listView.setOnItemLongClickListener(getNewLongClickViewFlipListener());
					listView.setAdapter(new ItemListAdapter(activity, R.id.tvStandard, result));
					loadingView.setVisibility(View.INVISIBLE);
					listView.setVisibility(View.VISIBLE);

					if (position == 0) buildTutorialView(activity, container, listView);
				}
			})
			.onError(new HandleViewIoException(activity, new OnConnectionRegainedListener() {

				@Override
				public void onConnectionRegained() {
					playlistsProvider.execute();
				}
			}));

		playlistsProvider.execute();

		return listView;
    }

	@SuppressWarnings("unchecked")
	private ListView BuildStandardItemView(final Activity activity, final ViewGroup container, final int position, final Item category, final View loadingView) {
		final ListView listView = new ListView(activity);
    	listView.setVisibility(View.INVISIBLE);

    	final ItemProvider itemProvider = new ItemProvider(category.getKey());

    	itemProvider.onComplete(new OnCompleteListener<Void, Void, List<Item>>() {

			@Override
			public void onComplete(ISimpleTask<Void, Void, List<Item>> owner, List<Item> result) {
				if (result == null) return;

                listView.setOnItemClickListener(new ClickItemListener(activity, result instanceof ArrayList ? (ArrayList<Item>)result : new ArrayList<>(result)));
                listView.setOnItemLongClickListener(getNewLongClickViewFlipListener());

                final ItemListAdapter<Item> itemListAdapter = new ItemListAdapter(activity, R.layout.layout_list_item, result);
		    	listView.setAdapter(itemListAdapter);
		    	loadingView.setVisibility(View.INVISIBLE);
	    		listView.setVisibility(View.VISIBLE);

                if (position == 0) buildTutorialView(activity, container, listView);
			}
		}).onError(new HandleViewIoException(activity, new OnConnectionRegainedListener() {

            @Override
            public void onConnectionRegained() {
                itemProvider.execute();
            }
        }));

    	itemProvider.execute();

		return listView;
	}

    private LongClickViewFlipListener getNewLongClickViewFlipListener() {
        final LongClickViewFlipListener longClickViewFlipListener = new LongClickViewFlipListener();
        if (mOnViewFlippedListener != null)
            longClickViewFlipListener.setOnViewFlipped(mOnViewFlippedListener);

        return longClickViewFlipListener;
    }

    public void setOnViewFlippedListener(OnViewFlippedListener onViewFlippedListener) {
        mOnViewFlippedListener = onViewFlippedListener;
    }

    private final static boolean DEBUGGING_TUTORIAL = false;
    private static void buildTutorialView(final Activity activity, final ViewGroup container, final ListView listView) {
        // use this flag to ensure the least amount of possible work is done for this tutorial
        if (wasTutorialShown) return;
        wasTutorialShown = true;

        final SharedPreferences sharedPreferences = activity.getSharedPreferences(ApplicationConstants.PREFS_FILE, 0);
        if (!DEBUGGING_TUTORIAL && sharedPreferences.getBoolean(PREFS_KEY, false)) return;

        int[] position = new int[2];
        container.getLocationOnScreen(position);

        final View childView = listView.getAdapter().getView(0, null, listView);
        childView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        final int measuredHeight = childView.getMeasuredHeight();

        // Put the view on the second item to make it clear we're talking about menu items
        final int topPosition = position[1] + measuredHeight + (measuredHeight / 2);
		new ShowcaseView.Builder(activity)
                .setTarget(new PointTarget(position[0], topPosition))
                .hideOnTouchOutside()
                .setContentTitle(R.string.title_long_click_menu)
                .setContentText(R.string.tutorial_long_click_menu)
                .build()
				.setBackgroundColor(activity.getResources().getColor(R.color.overlay_dark));

        sharedPreferences.edit().putBoolean(PREFS_KEY, true).apply();
    }
}