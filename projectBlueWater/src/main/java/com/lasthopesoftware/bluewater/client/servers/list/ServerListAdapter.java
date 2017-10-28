package com.lasthopesoftware.bluewater.client.servers.list;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.client.servers.list.listeners.EditServerClickListener;
import com.lasthopesoftware.bluewater.client.servers.list.listeners.SelectServerOnClickListener;
import com.lasthopesoftware.bluewater.client.servers.selection.BrowserLibrarySelection;
import com.lasthopesoftware.bluewater.client.servers.selection.IBrowserLibrarySelection;
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils;

import java.util.List;

public class ServerListAdapter extends BaseAdapter {

	private final List<Library> libraries;
	private final Library activeLibrary;
	private final IBrowserLibrarySelection browserLibrarySelection;
	private final Activity activity;

	private static class ViewHolder {
		final TextView textView;
		final Button btnSelectServer;
		final ImageButton btnConfigureServer;

		BroadcastReceiver broadcastReceiver;

		View.OnAttachStateChangeListener onAttachStateChangeListener;

		private ViewHolder(TextView textView, Button btnSelectServer, ImageButton btnConfigureServer) {
			this.textView = textView;
			this.btnSelectServer = btnSelectServer;
			this.btnConfigureServer = btnConfigureServer;
		}
	}

	public ServerListAdapter(Activity activity, List<Library> libraries, Library activeLibrary, IBrowserLibrarySelection browserLibrarySelection) {
		super();

		this.activity = activity;
		this.libraries = libraries;
		this.activeLibrary = activeLibrary;
		this.browserLibrarySelection = browserLibrarySelection;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final Context parentContext = parent.getContext();
		final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(parentContext);
		if (position == 0) {
			final RelativeLayout returnView = (RelativeLayout) getInflater(parentContext).inflate(R.layout.layout_standard_text, parent, false);
			final TextView textView = returnView.findViewById(R.id.tvStandard);
			textView.setText(parentContext.getText(R.string.btn_add_server));
			returnView.setOnClickListener(new EditServerClickListener(activity, -1));

			if (convertView != null && convertView.getTag() != null && ((ViewHolder)convertView.getTag()).broadcastReceiver != null)
				localBroadcastManager.unregisterReceiver(((ViewHolder) convertView.getTag()).broadcastReceiver);

			return returnView;
		}

		if (convertView == null || convertView.getTag() == null) {
			final RelativeLayout relativeLayout = (RelativeLayout) getInflater(parent.getContext()).inflate(R.layout.layout_server_item, parent, false);

			final TextView textView = relativeLayout.findViewById(R.id.tvServerItem);
			final Button btnSelectServer = relativeLayout.findViewById(R.id.btnSelectServer);
			final ImageButton btnConfigureServer = relativeLayout.findViewById(R.id.btnConfigureServer);

			relativeLayout.setTag(new ViewHolder(textView, btnSelectServer, btnConfigureServer));
			convertView = relativeLayout;
		}

		final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
		final Library library = libraries.get(position - 1);
		viewHolder.textView.setText(library.getAccessCode());

		final Button btnSelectServer = viewHolder.btnSelectServer;

		viewHolder.textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(activeLibrary != null && library.getId() == activeLibrary.getId()));

		if (viewHolder.broadcastReceiver != null)
			localBroadcastManager.unregisterReceiver(viewHolder.broadcastReceiver);

		viewHolder.broadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				viewHolder.textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(library.getId() == intent.getIntExtra(LibrarySession.chosenLibraryInt, -1)));
			}
		};

		localBroadcastManager.registerReceiver(viewHolder.broadcastReceiver, new IntentFilter(BrowserLibrarySelection.libraryChosenEvent));

		if (viewHolder.onAttachStateChangeListener != null)
			parent.removeOnAttachStateChangeListener(viewHolder.onAttachStateChangeListener);

		viewHolder.onAttachStateChangeListener = new View.OnAttachStateChangeListener() {
			@Override
			public void onViewAttachedToWindow(View v) {

			}

			@Override
			public void onViewDetachedFromWindow(View v) {
				localBroadcastManager.unregisterReceiver(viewHolder.broadcastReceiver);
			}
		};

		parent.addOnAttachStateChangeListener(viewHolder.onAttachStateChangeListener);

		btnSelectServer.setOnClickListener(new SelectServerOnClickListener(library, browserLibrarySelection));

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
