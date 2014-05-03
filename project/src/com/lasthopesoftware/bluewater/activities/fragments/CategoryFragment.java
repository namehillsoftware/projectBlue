package com.lasthopesoftware.bluewater.activities.fragments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import com.j256.ormlite.logger.LoggerFactory;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.ViewFiles;
import com.lasthopesoftware.bluewater.activities.WaitForConnection;
import com.lasthopesoftware.bluewater.activities.adapters.PlaylistAdapter;
import com.lasthopesoftware.bluewater.activities.adapters.views.BrowseItemMenu;
import com.lasthopesoftware.bluewater.activities.common.LongClickFlipListener;
import com.lasthopesoftware.bluewater.activities.listeners.ClickPlaylistListener;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.access.connection.PollConnectionTask;
import com.lasthopesoftware.bluewater.data.service.objects.IJrItem;
import com.lasthopesoftware.bluewater.data.service.objects.JrItem;
import com.lasthopesoftware.bluewater.data.service.objects.JrPlaylist;
import com.lasthopesoftware.bluewater.data.service.objects.JrPlaylists;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

public class CategoryFragment extends Fragment {
	private IJrItem<?> mCategory;
	private ListView listView;
	private ProgressBar pbLoading;
	private RelativeLayout mLayout;
	
	private ISimpleTask.OnCompleteListener<String, Void, ArrayList<IJrItem<?>>> mVisibleViewsComplete;
	private Context mContext;
	private Intent mWaitForConnection;
	
    public CategoryFragment() {
    	super();
    	
    	mVisibleViewsComplete = new ISimpleTask.OnCompleteListener<String, Void, ArrayList<IJrItem<?>>>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, ArrayList<IJrItem<?>>> owner, ArrayList<IJrItem<?>> result) {
				if (owner.getState() == SimpleTaskState.ERROR) {
					for (Exception exception : owner.getExceptions()) {
						if (!(exception instanceof IOException)) continue;
						
						PollConnectionTask.Instance.get().addOnCompleteListener(new ISimpleTask.OnCompleteListener<String, Void, Boolean>() {
							
							@Override
							public void onComplete(ISimpleTask<String, Void, Boolean> owner, Boolean result) {
								JrSession.JrFs.getVisibleViewsAsync(mVisibleViewsComplete);
							}
						});
						PollConnectionTask.Instance.get().startPolling();
						mContext.startActivity(mWaitForConnection);
						break;
					}
					return;
				}
				
				if (result == null) return;
				mCategory = result.get(getArguments().getInt(ARG_CATEGORY_POSITION));
				BuildView();
			}
		};
    }

    public static final String ARG_CATEGORY_POSITION = "category_position";
    public static final String IS_PLAYLIST = "Playlist";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	mLayout = new RelativeLayout(getActivity());
    	mLayout.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    	
    	mContext = mLayout.getContext();
    	mWaitForConnection = new Intent(mContext, WaitForConnection.class);
    	
    	pbLoading = new ProgressBar(mLayout.getContext(), null, android.R.attr.progressBarStyleLarge);
    	RelativeLayout.LayoutParams pbParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    	pbParams.addRule(RelativeLayout.CENTER_IN_PARENT);
    	pbLoading.setLayoutParams(pbParams);
    	mLayout.addView(pbLoading);
    	
    	JrSession.JrFs.getVisibleViewsAsync(mVisibleViewsComplete);
    	
        return mLayout;
    }
    
    private void BuildView() {
    	if (mCategory instanceof JrPlaylists) {
    		listView = new ListView(mLayout.getContext());
    		listView.setVisibility(View.INVISIBLE);
    		OnCompleteListener<List<JrPlaylist>> onPlaylistCompleteListener = new OnCompleteListener<List<JrPlaylist>>() {
				
				@Override
				public void onComplete(ISimpleTask<String, Void, List<JrPlaylist>> owner, List<JrPlaylist> result) {
					if (owner.getState() == SimpleTaskState.ERROR) {
						for (Exception exception : owner.getExceptions()) {
							if (!(exception instanceof IOException)) continue;
							
							PollConnectionTask.Instance.get().addOnCompleteListener(new ISimpleTask.OnCompleteListener<String, Void, Boolean>() {
								
								@Override
								public void onComplete(ISimpleTask<String, Void, Boolean> owner, Boolean result) {
									if (result)
										((JrPlaylists) mCategory).getSubItemsAsync();
								}
							});
							PollConnectionTask.Instance.get().startPolling();
							mContext.startActivity(mWaitForConnection);
							break;
						}
						return;
					}
					
					if (result == null) return;
					
					listView.setOnItemClickListener(new ClickPlaylistListener(getActivity(), (ArrayList<JrPlaylist>) result));
					listView.setOnItemLongClickListener(new LongClickFlipListener());
		    		listView.setAdapter(new PlaylistAdapter(getActivity(), R.id.tvStandard, result));
		    		pbLoading.setVisibility(View.INVISIBLE);
		    		listView.setVisibility(View.VISIBLE);					
				}
			};
			((JrPlaylists) mCategory).setOnItemsCompleteListener(onPlaylistCompleteListener);
			((JrPlaylists) mCategory).getSubItemsAsync();
    	} else {
	    	listView = new ExpandableListView(mLayout.getContext());
	    	listView.setVisibility(View.INVISIBLE);
	    	
	    	OnCompleteListener<List<JrItem>> onItemCompleteListener = new OnCompleteListener<List<JrItem>>() {

				@Override
				public void onComplete(ISimpleTask<String, Void, List<JrItem>> owner, List<JrItem> result) {
					if (owner.getState() == SimpleTaskState.ERROR) {
						for (Exception exception : owner.getExceptions()) {
							if (!(exception instanceof IOException)) continue;
							
							PollConnectionTask.Instance.get().addOnCompleteListener(new ISimpleTask.OnCompleteListener<String, Void, Boolean>() {
								
								@Override
								public void onComplete(ISimpleTask<String, Void, Boolean> owner, Boolean result) {
									if (result)
										((JrItem)mCategory).getSubItemsAsync();
								}
							});
							PollConnectionTask.Instance.get().startPolling();
							mContext.startActivity(mWaitForConnection);
							break;
						}
						return;
					}
					
					if (result == null) return;
					
					((ExpandableListView)listView).setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
						
						@Override
						public boolean onGroupClick(ExpandableListView parent, View v,
								int groupPosition, long id) {
							JrItem selection = (JrItem)parent.getExpandableListAdapter().getGroup(groupPosition);
							try {
								if (selection.getSubItems().size() > 0) return false;
							} catch (IOException e) {
								LoggerFactory.getLogger(CategoryFragment.class).warn(e.getMessage(), e);
								return true;
							}
				    		Intent intent = new Intent(parent.getContext(), ViewFiles.class);
				    		intent.setAction(ViewFiles.VIEW_ITEM_FILES);
				    		intent.putExtra(ViewFiles.KEY, selection.getKey());
				    		intent.putExtra(ViewFiles.VALUE, selection.getValue());
				    		startActivity(intent);
				    		return true;
						}
					});
			    	((ExpandableListView)listView).setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			    	    @Override
			    	    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {        	    	
			    	    	JrItem selection = (JrItem)parent.getExpandableListAdapter().getChild(groupPosition, childPosition);
				    		Intent intent = new Intent(parent.getContext(), ViewFiles.class);
				    		intent.setAction(ViewFiles.VIEW_ITEM_FILES);
				    		intent.putExtra(ViewFiles.KEY, selection.getKey());
				    		intent.putExtra(ViewFiles.VALUE, selection.getValue());
				    		startActivity(intent);
			    	        return true;
			    	    }
				    });
			    	listView.setOnItemLongClickListener(new LongClickFlipListener());
			    	
			    	((ExpandableListView)listView).setAdapter(new ExpandableItemListAdapter(getActivity(), (ArrayList<JrItem>)result));
			    	pbLoading.setVisibility(View.INVISIBLE);
		    		listView.setVisibility(View.VISIBLE);
				}
			};
			((JrItem)mCategory).setOnItemsCompleteListener(onItemCompleteListener);
			((JrItem)mCategory).getSubItemsAsync();
    	}
    	mLayout.addView(listView);
    }
    
    public static class ExpandableItemListAdapter extends BaseExpandableListAdapter {
    	Context mContext;
    	private ArrayList<JrItem> mCategoryItems;
    	
    	public ExpandableItemListAdapter(Context context, ArrayList<JrItem> categoryItems) {
    		mContext = context;
    		mCategoryItems = categoryItems;
    	}
    	
		@Override
		public Object getChild(int groupPosition, int childPosition) {
			try {
				return mCategoryItems.get(groupPosition).getSubItems().get(childPosition);
			} catch (IOException e) {
				 LoggerFactory.getLogger(CategoryFragment.class).warn(e.getMessage(), e);
				 return null;
			}
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			try {
				return ((JrItem)mCategoryItems.get(groupPosition).getSubItems().get(childPosition)).getKey();
			} catch (IOException e) {
				LoggerFactory.getLogger(CategoryFragment.class).warn(e.getMessage(), e);
				return 0;
			}
		}
		
		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			try {
				return BrowseItemMenu.getView(((JrItem)mCategoryItems.get(groupPosition).getSubItems().get(childPosition)), convertView, parent);
			} catch (IOException e) {
				LoggerFactory.getLogger(CategoryFragment.class).warn(e.getMessage(), e);
				return null;
			}
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			try {
				return mCategoryItems.get(groupPosition).getSubItems().size();
			} catch (IOException e) {
				LoggerFactory.getLogger(CategoryFragment.class).warn(e.getMessage(), e);
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
			final LayoutInflater inflator = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final RelativeLayout returnView = (RelativeLayout) inflator.inflate(R.layout.layout_standard_text, null);
			
			final TextView textView = (TextView) returnView.findViewById(R.id.tvStandard);			

	        textView.setPadding(64, 20, 20, 20);
		    textView.setText(mCategoryItems.get(groupPosition).getValue());

		    return returnView;
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