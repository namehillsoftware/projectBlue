package com.lasthopesoftware.bluewater.servers.list;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.library.BrowseLibraryActivity;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.servers.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.servers.list.listeners.EditServerClickListener;

import java.util.List;

public class ServerListAdapter extends BaseAdapter {

	private final List<Library> mLibraries;
	private final Activity mActivity;

	private static class ViewHolder {
		public final TextView textView;
		public final Button btnSelectServer;
		public final ImageButton btnConfigureServer;

		public BroadcastReceiver broadcastReceiver;

		private ViewHolder(TextView textView, Button btnSelectServer, ImageButton btnConfigureServer) {
			this.textView = textView;
			this.btnSelectServer = btnSelectServer;
			this.btnConfigureServer = btnConfigureServer;
		}
	}

	public ServerListAdapter(Activity activity, List<Library> libraries) {
		super();

		mActivity = activity;
		mLibraries = libraries;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final Context parentContext = parent.getContext();
		if (position == 0) {
			final RelativeLayout returnView = (RelativeLayout) getInflater(parentContext).inflate(R.layout.layout_standard_text, parent, false);
			final TextView textView = (TextView) returnView.findViewById(R.id.tvStandard);
			textView.setText(parentContext.getText(R.string.btn_add_server));
			returnView.setOnClickListener(new EditServerClickListener(mActivity, -1));

			if (convertView != null && convertView.getTag() != null && ((ViewHolder)convertView.getTag()).broadcastReceiver != null)
				LocalBroadcastManager.getInstance(parentContext).unregisterReceiver(((ViewHolder)convertView.getTag()).broadcastReceiver);

			return returnView;
		}

		if (convertView == null || convertView.getTag() == null) {
			final RelativeLayout relativeLayout = (RelativeLayout) getInflater(parent.getContext()).inflate(R.layout.layout_server_item, parent, false);

			final TextView textView = (TextView) relativeLayout.findViewById(R.id.tvServerItem);
			final Button btnSelectServer = (Button) relativeLayout.findViewById(R.id.btnSelectServer);
			final ImageButton btnConfigureServer = (ImageButton) relativeLayout.findViewById(R.id.btnConfigureServer);

			relativeLayout.setTag(new ViewHolder(textView, btnSelectServer, btnConfigureServer));
			convertView = relativeLayout;
		}

		final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
		final Library library = mLibraries.get(--position);
		viewHolder.textView.setText(library.getAccessCode());

		final Button btnSelectServer = viewHolder.btnSelectServer;

		btnSelectServer.setOnClickListener(v -> {
			final Context context = v.getContext();
			LibrarySession.ChangeActiveLibrary(context, library.getId(), null);

			final Intent browseLibraryIntent = new Intent(context, BrowseLibraryActivity.class);
			browseLibraryIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(browseLibraryIntent);
		});

		viewHolder.btnConfigureServer.setOnClickListener(new EditServerClickListener(mActivity, library.getId()));

		return convertView;
	}

	private static LayoutInflater getInflater(Context context) {
		return (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
