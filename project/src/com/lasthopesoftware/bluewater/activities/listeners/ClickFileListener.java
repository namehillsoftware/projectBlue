package com.lasthopesoftware.bluewater.activities.listeners;

import java.io.IOException;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.lasthopesoftware.bluewater.activities.common.WaitForConnectionDialog;
import com.lasthopesoftware.bluewater.data.service.access.connection.PollConnectionTask;
import com.lasthopesoftware.bluewater.data.service.access.connection.PollConnectionTask.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.data.service.objects.IItemFiles;
import com.lasthopesoftware.bluewater.services.StreamingMusicService;

public class ClickFileListener implements OnItemClickListener {

	private IItemFiles mItem;
	
	public ClickFileListener(IItemFiles item) {
		mItem = item;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		
		try {
			StreamingMusicService.streamMusic(view.getContext(), position, mItem.getFileStringList());
		} catch (IOException io) {
			final AdapterView<?> _parent = parent;
			final View _view = view;
			final int _position = position;
			final long _id = id;
			
			PollConnectionTask.Instance.get(view.getContext()).addOnConnectionRegainedListener(new OnConnectionRegainedListener() {
				
				@Override
				public void onConnectionRegained() {
					onItemClick(_parent, _view, _position, _id);
				}
			});
			
			WaitForConnectionDialog.show(view.getContext());
		}
	}

}
