package com.lasthopesoftware.bluewater.activities.listeners;

import java.io.IOException;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.lasthopesoftware.bluewater.activities.common.WaitForConnectionDialog;
import com.lasthopesoftware.bluewater.data.service.access.connection.PollConnectionTask;
import com.lasthopesoftware.bluewater.data.service.objects.IJrItemFiles;
import com.lasthopesoftware.bluewater.services.StreamingMusicService;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

public class ClickFileListener implements OnItemClickListener {

	private IJrItemFiles mItem;
	
	public ClickFileListener(IJrItemFiles item) {
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
			
			PollConnectionTask.Instance.get(view.getContext()).addOnCompleteListener(new OnCompleteListener<String, Void, Boolean>() {
				
				@Override
				public void onComplete(ISimpleTask<String, Void, Boolean> owner, Boolean result) {
					if (result)
						onItemClick(_parent, _view, _position, _id);
				}
			});
			
			WaitForConnectionDialog.show(view.getContext());
		}
	}

}
