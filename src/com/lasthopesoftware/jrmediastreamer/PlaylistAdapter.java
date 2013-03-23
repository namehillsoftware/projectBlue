package com.lasthopesoftware.jrmediastreamer;

import java.util.ArrayList;

import jrAccess.JrSession;
import jrFileSystem.JrFile;
import jrFileSystem.JrItem;
import jrFileSystem.JrPlaylist;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class PlaylistAdapter extends BaseAdapter {
	private ArrayList<JrPlaylist> mPlaylists;
	private Context mContext;
	
	public PlaylistAdapter(Context context, ArrayList<JrPlaylist> playlists) {
		mPlaylists = playlists;
		mContext = context;
	}
	
	@Override
	public int getCount() {
		return mPlaylists.size();
	}

	@Override
	public Object getItem(int position) {
		return mPlaylists.get(position);
	}

	@Override
	public long getItemId(int position) {
		return mPlaylists.get(position).getKey();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return BrowseItemMenu.getView(mPlaylists.get(position), convertView, parent);
	}
}
