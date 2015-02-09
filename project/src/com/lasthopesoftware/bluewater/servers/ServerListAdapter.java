package com.lasthopesoftware.bluewater.servers;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.Library;

public class ServerListAdapter extends BaseAdapter {
	private List<Library> mLibraries;
	
	public ServerListAdapter(Context context, List<Library> libraries) {
		super();
		mLibraries = libraries;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final LayoutInflater inflator = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final RelativeLayout returnView = (RelativeLayout) inflator.inflate(R.layout.layout_standard_text, null);
		
		final TextView textView = (TextView) returnView.findViewById(R.id.tvStandard);
		textView.setText(position == 0 ? "Add Server" : mLibraries.get(--position).getAccessCode());
		
		return returnView;
	}

	@Override
	public int getCount() {
		return mLibraries.size() + 1;
	}

	@Override
	public Object getItem(int position) {
		return mLibraries.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position > 0 ? mLibraries.get(--position).getId() : -1;
	}
}
