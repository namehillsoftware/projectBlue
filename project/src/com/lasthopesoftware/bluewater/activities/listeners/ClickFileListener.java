package com.lasthopesoftware.bluewater.activities.listeners;

import java.io.IOException;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.lasthopesoftware.bluewater.activities.common.WaitForConnectionDialog;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.data.service.objects.IItemFiles;
import com.lasthopesoftware.bluewater.services.StreamingMusicService;
import com.lasthopesoftware.threading.ISimpleTask;

public class ClickFileListener implements OnItemClickListener {

	private IItemFiles mItem;
	
	public ClickFileListener(IItemFiles item) {
		mItem = item;
	}
	
	@Override
	public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
		mItem.getFileStringList(new ISimpleTask.OnCompleteListener<String, Void, String>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, String> owner, String result) {
				for (Exception exception : owner.getExceptions()) {
					if (exception instanceof IOException) {
						PollConnection.Instance.get(view.getContext()).addOnConnectionRegainedListener(new OnConnectionRegainedListener() {
							
							@Override
							public void onConnectionRegained() {
								onItemClick(parent, view, position, id);
							}
						});
						
						WaitForConnectionDialog.show(view.getContext());
						return;
					}
				}
				
				StreamingMusicService.streamMusic(view.getContext(), result);
			}
		});
	}

}
