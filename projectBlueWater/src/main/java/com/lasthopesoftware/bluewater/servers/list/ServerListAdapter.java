package com.lasthopesoftware.bluewater.servers.list;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
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
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.servers.list.listeners.EditServerClickListener;
import com.lasthopesoftware.bluewater.servers.list.listeners.SelectServerOnClickListener;

import java.util.List;

public class ServerListAdapter extends BaseAdapter {

	private final List<Library> libraries;
	private final Activity activity;

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

		this.activity = activity;
		this.libraries = libraries;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final Context parentContext = parent.getContext();
		if (position == 0) {
			final RelativeLayout returnView = (RelativeLayout) getInflater(parentContext).inflate(R.layout.layout_standard_text, parent, false);
			final TextView textView = (TextView) returnView.findViewById(R.id.tvStandard);
			textView.setText(parentContext.getText(R.string.btn_add_server));
			returnView.setOnClickListener(new EditServerClickListener(activity, -1));

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
		final Library library = libraries.get(--position);
		viewHolder.textView.setText(library.getAccessCode());

		final Button btnSelectServer = viewHolder.btnSelectServer;

		btnSelectServer.setOnClickListener(new SelectServerOnClickListener(library));

		viewHolder.btnConfigureServer.setOnClickListener(new EditServerClickListener(activity, library.getId()));

		return convertView;
	}

	private static LayoutInflater getInflater(Context context) {
		return (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return libraries.size() + 1;
	}

	@Override
	public Object getItem(int position) {
		return libraries.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position > 0 ? libraries.get(--position).getId() : -1;
	}

}
