package com.lasthopesoftware.bluewater.activities.listeners;

import java.io.IOException;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.lasthopesoftware.bluewater.activities.common.WaitForConnectionDialog;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.data.service.objects.IItemFiles;
import com.lasthopesoftware.bluewater.services.StreamingMusicService;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

public class ClickFileListener implements OnItemClickListener {

	private final IItemFiles mItemFiles;
	
	public ClickFileListener(IItemFiles itemFiles) {
		mItemFiles = itemFiles;
	}
	
	@Override
	public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
		mItemFiles.getFileStringList(new OnCompleteListener<String>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, String> owner, String result) {
				if (owner.getState() == SimpleTaskState.ERROR) return;
				
				StreamingMusicService.streamMusic(view.getContext(), position, result);
			}
		}, new OnErrorListener<String>() {

			@Override
			public boolean onError(ISimpleTask<String, Void, String> owner, boolean isHandled, Exception innerException) {
				if (!(innerException instanceof IOException)) return false;
				
				PollConnection.Instance.get(view.getContext()).addOnConnectionRegainedListener(new OnConnectionRegainedListener() {
					
					@Override
					public void onConnectionRegained() {
						onItemClick(parent, view, position, id);
					}
				});
				
				WaitForConnectionDialog.show(view.getContext());
				return true;
			}
		});
	}
}
