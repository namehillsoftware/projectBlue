package com.lasthopesoftware.bluewater.servers;

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
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.Library;
import com.lasthopesoftware.threading.ISimpleTask;

import java.util.List;

public class ServerListAdapter extends BaseAdapter {

	private final List<Library> mLibraries;
	private Library mChosenLibrary;

	private static class ViewHolder {
		public final TextView textView;
		public final Switch selectServerSwitch;
		public BroadcastReceiver broadcastReceiver;

		private ViewHolder(TextView textView, Switch selectServerSwitch) {
			this.textView = textView;
			this.selectServerSwitch = selectServerSwitch;
		}
	}

	public ServerListAdapter(List<Library> libraries, Library chosenLibrary) {
		super();

		mLibraries = libraries;
		mChosenLibrary = chosenLibrary;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final Context parentContext = parent.getContext();
		if (position == 0) {
			final RelativeLayout returnView = (RelativeLayout) getInflater(parentContext).inflate(R.layout.layout_standard_text, null);
			final TextView textView = (TextView) returnView.findViewById(R.id.tvStandard);
			textView.setText("Add Server");
			returnView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(final View v) {
					LibrarySession.ChooseLibrary(v.getContext(), -1, new ISimpleTask.OnCompleteListener<Integer, Void, Library>() {

						@Override
						public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
							v.getContext().startActivity(new Intent(v.getContext(), EditServerActivity.class));
						}
					});
				}
			});

			if (convertView != null && convertView.getTag() != null)
				LocalBroadcastManager.getInstance(parentContext).unregisterReceiver(((ViewHolder)convertView.getTag()).broadcastReceiver);

			return returnView;
		}

		if (convertView == null || convertView.getTag() == null) {
			final RelativeLayout relativeLayout = (RelativeLayout) getInflater(parent.getContext()).inflate(R.layout.layout_server_item, null);

			final TextView textView = (TextView) relativeLayout.findViewById(R.id.tvServerItem);
			final Switch selectServerSwitch = (Switch) relativeLayout.findViewById(R.id.selectServerSwitch);

			relativeLayout.setTag(new ViewHolder(textView, selectServerSwitch));
			convertView = relativeLayout;
		}

		final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
		final Library library = mLibraries.get(--position);
		viewHolder.textView.setText(library.getAccessCode());

		final Switch selectServerSwitch = viewHolder.selectServerSwitch;
		if (library.getId() == mChosenLibrary.getId())
			selectServerSwitch.setChecked(true);

		selectServerSwitch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final Context context = v.getContext();
				LibrarySession.ChooseLibrary(context, library.getId(), null);
			}
		});

		final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(parentContext);
		if (viewHolder.broadcastReceiver != null) localBroadcastManager.unregisterReceiver(viewHolder.broadcastReceiver);

		viewHolder.broadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				final int libraryChosenInt = intent.getIntExtra(LibrarySession.chosenLibraryInt, -1);
				selectServerSwitch.setChecked(libraryChosenInt == library.getId());
			}
		};

		localBroadcastManager.registerReceiver(viewHolder.broadcastReceiver, new IntentFilter(LibrarySession.libraryChosenEvent));

		selectServerSwitch.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
			@Override
			public void onViewAttachedToWindow(View v) {

			}

			@Override
			public void onViewDetachedFromWindow(View v) {
				if (viewHolder.broadcastReceiver != null)
					localBroadcastManager.unregisterReceiver(viewHolder.broadcastReceiver);
				selectServerSwitch.removeOnAttachStateChangeListener(this);
			}
		});

		return convertView;
	}

	private static LayoutInflater getInflater(Context context) {
		return (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	private static Drawable mSelectedServerDrawable;
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
