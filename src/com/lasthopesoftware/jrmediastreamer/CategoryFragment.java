package com.lasthopesoftware.jrmediastreamer;

import java.util.ArrayList;

import jrAccess.JrSession;
import jrFileSystem.JrItem;
import jrFileSystem.JrPlaylists;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;

public class CategoryFragment extends Fragment {
    public CategoryFragment() {
    	super();
    }

    public static final String ARG_CATEGORY_POSITION = "category_position";
    public static final String IS_PLAYLIST = "playlist";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

    	int categoryPosition = getArguments().getInt(ARG_CATEGORY_POSITION);
    	if (JrSession.categories.get(categoryPosition) instanceof JrPlaylists) {
    		ListView playlistView = new ListView(getActivity());
    		playlistView.setOnItemClickListener(new ClickPlaylist(getActivity(), JrSession.categories.get(categoryPosition).getSubItems()));
    		playlistView.setAdapter(new PlaylistAdapter(getActivity(), JrSession.categories.get(categoryPosition).getSubItems()));
    		return playlistView;
    	}
    	
    	ExpandableListView listView = new ExpandableListView(getActivity());
    	ExpandableItemListAdapter adapter = new ExpandableItemListAdapter(getActivity(), categoryPosition);
    	listView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
			
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v,
					int groupPosition, long id) {
				JrItem selection = (JrItem)parent.getExpandableListAdapter().getGroup(groupPosition);
				if (selection.getSubItems().size() > 0) return false;
	    		Intent intent = new Intent(parent.getContext(), ViewFiles.class);
	    		intent.setAction(ViewFiles.VIEW_ITEM_FILES);
	    		intent.putExtra(ViewFiles.KEY, selection.getKey());
	    		JrSession.selectedItem = selection;
	    		startActivity(intent);
	    		return true;
			}
		});
    	listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
    	    @Override
    	    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {        	    	
    	    	JrItem selection = (JrItem)parent.getExpandableListAdapter().getChild(groupPosition, childPosition);
	    		Intent intent = new Intent(parent.getContext(), ViewFiles.class);
	    		JrSession.selectedItem = selection;
	    		startActivity(intent);
    	        return true;
    	    }
	    });
    	listView.setAdapter(adapter);
        return listView;
    }
    
    public static class ExpandableItemListAdapter extends BaseExpandableListAdapter {
    	Context mContext;
    	private ArrayList<JrItem> mCategoryItems;
    	
    	public ExpandableItemListAdapter(Context context, int CategoryPosition) {
    		mContext = context;
    		mCategoryItems = ((JrItem)JrSession.categories.get(CategoryPosition)).getSubItems();
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
		public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
			TextView returnView = getGenericView(mContext);
	//			tv.setGravity(Gravity.LEFT);
			returnView.setText(((JrItem)mCategoryItems.get(groupPosition).getSubItems().get(childPosition)).getValue());
			return returnView;
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
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {

			TextView tv = getGenericView(mContext);
			tv.setText(mCategoryItems.get(groupPosition).getValue());
			
			return tv;
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
    
    public static TextView getGenericView(Context context) {
        // Layout parameters for the ExpandableListView
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView textView = new TextView(context);
        textView.setTextAppearance(context, android.R.style.TextAppearance_Large);
        textView.setLayoutParams(lp);
        // Center the text vertically
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
//        textView.setTextColor(getResources().getColor(marcyred));
        // Set the text starting position        
        textView.setPadding(64, 20, 20, 20);
        //textView.setHeight(textView.getLineHeight() + 20);
        return textView;
    }
}