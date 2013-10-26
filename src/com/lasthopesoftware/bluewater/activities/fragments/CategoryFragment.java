package com.lasthopesoftware.bluewater.activities.fragments;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils.TruncateAt;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.activities.ViewFiles;
import com.lasthopesoftware.bluewater.activities.adapters.PlaylistAdapter;
import com.lasthopesoftware.bluewater.activities.common.BrowseItemMenu;
import com.lasthopesoftware.bluewater.activities.listeners.ClickPlaylistListener;
import com.lasthopesoftware.bluewater.data.access.IJrDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.objects.IJrItem;
import com.lasthopesoftware.bluewater.data.objects.JrItem;
import com.lasthopesoftware.bluewater.data.objects.JrPlaylist;
import com.lasthopesoftware.bluewater.data.objects.JrPlaylists;
import com.lasthopesoftware.bluewater.data.objects.JrSession;
import com.lasthopesoftware.threading.ISimpleTask;

public class CategoryFragment extends Fragment {
	private IJrItem<?> mCategory;
	private ListView listView;
	private ProgressBar pbLoading;
	
    public CategoryFragment() {
    	super();
    }

    public static final String ARG_CATEGORY_POSITION = "category_position";
    public static final String IS_PLAYLIST = "Playlist";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	RelativeLayout layout = new RelativeLayout(getActivity());
    	layout.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    	
    	pbLoading = new ProgressBar(layout.getContext(), null, android.R.attr.progressBarStyleLarge);
    	RelativeLayout.LayoutParams pbParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    	pbParams.addRule(RelativeLayout.CENTER_IN_PARENT);
    	pbLoading.setLayoutParams(pbParams);
    	layout.addView(pbLoading);
    	
    	mCategory = JrSession.getCategoriesList().get(getArguments().getInt(ARG_CATEGORY_POSITION));
    	
    	if (mCategory instanceof JrPlaylists) {
    		listView = new ListView(layout.getContext());
    		listView.setVisibility(View.INVISIBLE);
    		OnCompleteListener<List<JrPlaylist>> onPlaylistCompleteListener = new OnCompleteListener<List<JrPlaylist>>() {
				
				@Override
				public void onComplete(ISimpleTask<String, Void, List<JrPlaylist>> owner, List<JrPlaylist> result) {
					listView.setOnItemClickListener(new ClickPlaylistListener(getActivity(), (ArrayList<JrPlaylist>) result));
					listView.setOnItemLongClickListener(new BrowseItemMenu.ClickListener());
		    		listView.setAdapter(new PlaylistAdapter((ArrayList<JrPlaylist>) result));
		    		pbLoading.setVisibility(View.INVISIBLE);
		    		listView.setVisibility(View.VISIBLE);					
				}
			};
			((JrPlaylists) mCategory).setOnItemsCompleteListener(onPlaylistCompleteListener);
			((JrPlaylists) mCategory).getSubItemsAsync();
    	} else {
	    	listView = new ExpandableListView(layout.getContext());
	    	listView.setVisibility(View.INVISIBLE);
	    	
	    	OnCompleteListener<List<JrItem>> onItemCompleteListener = new OnCompleteListener<List<JrItem>>() {

				@Override
				public void onComplete(ISimpleTask<String, Void, List<JrItem>> owner, List<JrItem> result) {
					((ExpandableListView)listView).setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
						
						@Override
						public boolean onGroupClick(ExpandableListView parent, View v,
								int groupPosition, long id) {
							JrItem selection = (JrItem)parent.getExpandableListAdapter().getGroup(groupPosition);
							if (selection.getSubItems().size() > 0) return false;
				    		Intent intent = new Intent(parent.getContext(), ViewFiles.class);
				    		intent.setAction(ViewFiles.VIEW_ITEM_FILES);
				    		intent.putExtra(ViewFiles.KEY, selection.getKey());
				    		intent.putExtra(ViewFiles.VALUE, selection.getValue());
				    		JrSession.SelectedItem = selection;
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
				    		JrSession.SelectedItem = selection;
				    		startActivity(intent);
			    	        return true;
			    	    }
				    });
			    	listView.setOnItemLongClickListener(new BrowseItemMenu.ClickListener());
			    	
			    	((ExpandableListView)listView).setAdapter(new ExpandableItemListAdapter(getActivity(), (ArrayList<JrItem>)result));
			    	pbLoading.setVisibility(View.INVISIBLE);
		    		listView.setVisibility(View.VISIBLE);
				}
			};
			((JrItem)mCategory).setOnItemsCompleteListener(onItemCompleteListener);
			((JrItem)mCategory).getSubItemsAsync();
    	}
    	layout.addView(listView);
        return layout;
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
			return mCategoryItems.get(groupPosition).getSubItems().get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return ((JrItem)mCategoryItems.get(groupPosition).getSubItems().get(childPosition)).getKey();
		}
		
		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			return BrowseItemMenu.getView(((JrItem)mCategoryItems.get(groupPosition).getSubItems().get(childPosition)), convertView, parent);
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return mCategoryItems.get(groupPosition).getSubItems().size();
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

			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

	        TextView textView = new TextView(mContext);
	        textView.setTextAppearance(mContext, android.R.style.TextAppearance_Large);
	        textView.setLayoutParams(lp);
	        // Center the text vertically
	        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
//		        textView.setTextColor(getResources().getColor(marcyred));
	        // Set the text starting position        
	        textView.setPadding(64, 20, 20, 20);
	        textView.setEllipsize(TruncateAt.END);
	        textView.setSingleLine();
	        textView.setMarqueeRepeatLimit(1);
//	        textView.setPadding(20, 20, 20, 20);
		    textView.setText(mCategoryItems.get(groupPosition).getValue());
	        
//		    if (getChildrenCount(groupPosition) < 1) {
//		    	
//		    }
		    
			return textView;
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