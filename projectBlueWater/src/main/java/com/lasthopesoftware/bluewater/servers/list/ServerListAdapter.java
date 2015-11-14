package com.lasthopesoftware.bluewater.servers.list;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.servers.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.servers.list.listeners.EditServerClickListener;

import java.util.List;

public class ServerListAdapter extends BaseAdapter {

	private final List<Library> mLibraries;
	private Library mChosenLibrary;
	private static Drawable mSelectedServerDrawable;
	private static Drawable mNotSelectedServerDrawable;
	private final Activity mActivity;

	private static class ViewHolder {
		public final TextView textView;
		public final ImageButton btnSelectServer;
		public final ImageButton btnConfigureServer;

		public BroadcastReceiver broadcastReceiver;

		private ViewHolder(TextView textView, ImageButton btnSelectServer, ImageButton btnConfigureServer) {
			this.textView = textView;
			this.btnSelectServer = btnSelectServer;
			this.btnConfigureServer = btnConfigureServer;
		}
	}

	public ServerListAdapter(Activity activity, List<Library> libraries, Library chosenLibrary) {
		super();

		mActivity = activity;
		mLibraries = libraries;
		mChosenLibrary = chosenLibrary;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final Context parentContext = parent.getContext();
		if (position == 0) {
			final RelativeLayout returnView = (RelativeLayout) getInflater(parentContext).inflate(R.layout.layout_standard_text, null);
			final TextView textView = (TextView) returnView.findViewById(R.id.tvStandard);
			textView.setText(parentContext.getText(R.string.btn_add_server));
			returnView.setOnClickListener(new EditServerClickListener(mActivity, -1));

			if (convertView != null && convertView.getTag() != null && ((ViewHolder)convertView.getTag()).broadcastReceiver != null)
				LocalBroadcastManager.getInstance(parentContext).unregisterReceiver(((ViewHolder)convertView.getTag()).broadcastReceiver);

			return returnView;
		}

		if (convertView == null || convertView.getTag() == null) {
			final RelativeLayout relativeLayout = (RelativeLayout) getInflater(parent.getContext()).inflate(R.layout.layout_server_item, null);

			final TextView textView = (TextView) relativeLayout.findViewById(R.id.tvServerItem);
			final ImageButton btnSelectServer = (ImageButton) relativeLayout.findViewById(R.id.btnSelectServer);
			final ImageButton btnConfigureServer = (ImageButton) relativeLayout.findViewById(R.id.btnConfigureServer);

			relativeLayout.setTag(new ViewHolder(textView, btnSelectServer, btnConfigureServer));
			convertView = relativeLayout;
		}

		final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
		final Library library = mLibraries.get(--position);
		viewHolder.textView.setText(library.getAccessCode());

		final ImageButton btnSelectServer = viewHolder.btnSelectServer;
		if (mChosenLibrary != null && library.getId() == mChosenLibrary.getId())
			btnSelectServer.setImageDrawable(getSelectedServerDrawable(parentContext));

		btnSelectServer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final Context context = v.getContext();
				LibrarySession.ChooseLibrary(context, library.getId(), null);
			}
		});

		viewHolder.btnConfigureServer.setOnClickListener(new EditServerClickListener(mActivity, library.getId()));

		final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(parentContext);
		if (viewHolder.broadcastReceiver != null) localBroadcastManager.unregisterReceiver(viewHolder.broadcastReceiver);

		viewHolder.broadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				final boolean isChosen = intent.getIntExtra(LibrarySession.chosenLibraryInt, -1) == library.getId();
				btnSelectServer.setImageDrawable(isChosen ? getSelectedServerDrawable(context) : getNotSelectedServerDrawable(context));
			}
		};

		localBroadcastManager.registerReceiver(viewHolder.broadcastReceiver, new IntentFilter(LibrarySession.libraryChosenEvent));

		btnSelectServer.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
			@Override
			public void onViewAttachedToWindow(View v) {

			}

			@Override
			public void onViewDetachedFromWindow(View v) {
				if (viewHolder.broadcastReceiver != null)
					localBroadcastManager.unregisterReceiver(viewHolder.broadcastReceiver);
				btnSelectServer.removeOnAttachStateChangeListener(this);
			}
		});

		return convertView;
	}

	private static LayoutInflater getInflater(Context context) {
		return (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	private static Drawable getNotSelectedServerDrawable(Context context) {
		if (mNotSelectedServerDrawable == null)
			mNotSelectedServerDrawable = context.getResources().getDrawable(R.drawable.ic_checkbox_blank_circle_outline_grey600_24dp);

		return mNotSelectedServerDrawable;
	}

	private static Drawable getSelectedServerDrawable(Context context) {
		if (mSelectedServerDrawable == null)
			mSelectedServerDrawable = context.getResources().getDrawable(R.drawable.ic_checkbox_marked_circle_grey600_24dp);

		return mSelectedServerDrawable;
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

	public interface OnServerSelected {
		void onServerSelected(Context context, Library library);
	}
}
