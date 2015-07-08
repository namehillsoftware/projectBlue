package com.lasthopesoftware.bluewater.servers;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.servers.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.threading.ISimpleTask;

import java.util.List;

public class ServerListAdapter extends BaseAdapter {

	private final List<Library> mLibraries;
	private Library mChosenLibrary;
	private OnServerSelected mOnServerSelected;

	private static Drawable mSelectedServerDrawable;

	public ServerListAdapter(List<Library> libraries, Library chosenLibrary) {
		super();

		mLibraries = libraries;
		mChosenLibrary = chosenLibrary;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final LayoutInflater inflator = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final RelativeLayout returnView = (RelativeLayout) inflator.inflate(position == 0 ? R.layout.layout_standard_text : R.layout.layout_server_item, null);
		if (position == 0) {
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
			return returnView;
		}

		final Library library = mLibraries.get(--position);
		final TextView textView = (TextView) returnView.findViewById(R.id.tvServerItem);
		textView.setText(library.getAccessCode());

		final ImageButton selectServerButton = (ImageButton) returnView.findViewById(R.id.btnSelectServer);

		if (library.getId() == mChosenLibrary.getId())
			selectServerButton.setImageDrawable(getSelectedServerDrawable(parent.getContext()));

		selectServerButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final Context context = v.getContext();
				LibrarySession.ChooseLibrary(context, library.getId(), new ISimpleTask.OnCompleteListener<Integer, Void, Library>() {

					@Override
					public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
						selectServerButton.setImageDrawable(getSelectedServerDrawable(context));
						context.startActivity(new Intent(context, InstantiateSessionConnectionActivity.class));

						if (mOnServerSelected != null)
							mOnServerSelected.onServerSelected(context, result);
					}
				});
			}
		});

		return returnView;
	}

	private static Drawable getSelectedServerDrawable(Context context) {
		if (mSelectedServerDrawable == null)
			mSelectedServerDrawable = context.getResources().getDrawable(R.drawable.ic_checkbox_marked_circle_grey600_24dp);

		return mSelectedServerDrawable;
	}

	public void setOnServerSelected(OnServerSelected onServerSelected) {
		mOnServerSelected = onServerSelected;
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
