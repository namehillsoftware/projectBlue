package com.lasthopesoftware.bluewater.servers.library;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.data.service.objects.FileSystem;
import com.lasthopesoftware.bluewater.data.service.objects.FileSystem.OnGetFileSystemCompleteListener;
import com.lasthopesoftware.bluewater.data.service.objects.IItem;
import com.lasthopesoftware.bluewater.data.service.objects.Item;
import com.lasthopesoftware.bluewater.servers.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.servers.library.items.ItemMenu;
import com.lasthopesoftware.bluewater.servers.library.items.ItemProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.list.FileListActivity;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.ClickPlaylistListener;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.PlaylistListAdapter;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlists;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.PlaylistsProvider;
import com.lasthopesoftware.bluewater.shared.listener.LongClickFlipListener;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

public class CategoryFragment extends Fragment {
	
    public static final String ARG_CATEGORY_POSITION = "category_position";
    public static final String IS_PLAYLIST = "Playlist";
	    
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
						if (result == null) return;
						
						final IItem category = result.get(getArguments().getInt(ARG_CATEGORY_POSITION));
										
						if (category instanceof Playlists)
							layout.addView(BuildPlaylistView(context, (Playlists)category, pbLoading));
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
	private ListView BuildPlaylistView(final Context context, final Playlists category, final View loadingView) {
    	
		final ListView listView = new ListView(context);
		listView.setVisibility(View.INVISIBLE);
		final PlaylistsProvider playlistsProvider = new PlaylistsProvider("Playlists/List");
		playlistsProvider
			.onComplete(new OnCompleteListener<Void, Void, List<Playlist>>() {
				
				@Override
				public void onComplete(ISimpleTask<Void, Void, List<Playlist>> owner, List<Playlist> result) {
					if (result == null) return;
					
					listView.setOnItemClickListener(new ClickPlaylistListener(context, (ArrayList<Playlist>) result));
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
	private ExpandableListView BuildStandardItemView(final Context context, final Item category, final View loadingView) {
		final ExpandableListView listView = new ExpandableListView(context);
    	listView.setVisibility(View.INVISIBLE);
    	
    	final ItemProvider itemProvider = new ItemProvider(category.getSubItemParams());
    	
    	itemProvider.onComplete(new OnCompleteListener<Void, Void, List<Item>>() {

			@Override
			public void onComplete(ISimpleTask<Void, Void, List<Item>> owner, List<Item> result) {
				if (result == null) return;
				
				listView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
					
					@Override
					public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
						final Item selection = (Item)parent.getExpandableListAdapter().getGroup(groupPosition);
						
						try {
							if ((new ItemProvider(selection.getSubItemParams())).get().size() > 0) return false;
						} catch (ExecutionException | InterruptedException e) {
							LoggerFactory.getLogger(CategoryFragment.class).warn(e.getMessage(), e);
							return true;
						}
					
						final Intent intent = new Intent(parent.getContext(), FileListActivity.class);
			    		intent.setAction(FileListActivity.VIEW_ITEM_FILES);
			    		intent.putExtra(FileListActivity.KEY, selection.getKey());
			    		intent.putExtra(FileListActivity.VALUE, selection.getValue());
			    		startActivity(intent);
			    		return true;
					}
				});
		    	listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
		    	    @Override
		    	    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {        	    	
		    	    	final Item selection = (Item)parent.getExpandableListAdapter().getChild(groupPosition, childPosition);
			    		final Intent intent = new Intent(parent.getContext(), FileListActivity.class);
			    		intent.setAction(FileListActivity.VIEW_ITEM_FILES);
			    		intent.putExtra(FileListActivity.KEY, selection.getKey());
			    		intent.putExtra(FileListActivity.VALUE, selection.getValue());
			    		startActivity(intent);
		    	        return true;
		    	    }
			    });
		    	listView.setOnItemLongClickListener(new LongClickFlipListener());
		    	
		    	listView.setAdapter(new ExpandableItemListAdapter(result));
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

    public static class ExpandableItemListAdapter extends BaseExpandableListAdapter {
    	private final ArrayList<Item> mCategoryItems;
    	private final SparseArray<List<Item>> mChildren = new SparseArray<List<Item>>();
    	
    	private final static Logger mLogger = LoggerFactory.getLogger(ExpandableItemListAdapter.class);
    	
    	public ExpandableItemListAdapter(List<Item> categoryItems) {
    		mCategoryItems = new ArrayList<Item>(categoryItems);
    	}
    	
    	private List<Item> getChildren(int position) throws ExecutionException, InterruptedException {
    		List<Item> children = mChildren.get(position);
    		if (children == null) {
	    		children = (new ItemProvider(mCategoryItems.get(position).getSubItemParams())).get();
	    		mChildren.put(position, children);
    		}
    		return children;
    	}
    	
		@Override
		public Object getChild(int groupPosition, int childPosition) {
			try {
				return getChildren(groupPosition).get(childPosition);
			} catch (ExecutionException | InterruptedException /*| IOException*/ e) {
				mLogger.warn(e.getMessage(), e);
				return null;
			}
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return ((Item)getChild(groupPosition, childPosition)).getKey();
		}
		
		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			return ItemMenu.getView((Item)getChild(groupPosition, childPosition), convertView, parent);
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			try {
				return getChildren(groupPosition).size();
			} catch (ExecutionException | InterruptedException e) {
				mLogger.warn(e.getMessage(), e);
				return 0;
			}
		}

		@Override
		public Object getGroup(int groupPosition) {
			return mCategoryItems.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return mCategoryItems.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return mCategoryItems.get(groupPosition).getKey();
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			if (convertView == null) {
				final LayoutInflater inflator = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = (RelativeLayout) inflator.inflate(R.layout.layout_standard_text, parent, false);
				
				final TextView heldTextView = (TextView) convertView.findViewById(R.id.tvStandard);			
	
				heldTextView.setPadding(64, 20, 20, 20);
		        
		        convertView.setTag(heldTextView);
			}
			
			((TextView)convertView.getTag()).setText(mCategoryItems.get(groupPosition).getValue());

		    return convertView;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
    	
    }
}