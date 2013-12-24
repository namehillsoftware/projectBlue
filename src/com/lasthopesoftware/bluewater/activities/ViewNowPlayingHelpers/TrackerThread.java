package com.lasthopesoftware.bluewater.activities.ViewNowPlayingHelpers;

import com.lasthopesoftware.bluewater.data.access.connection.PollConnectionTask;
import com.lasthopesoftware.bluewater.data.objects.JrFile;

import android.os.Message;


public class TrackerThread implements Runnable {
		private JrFile mFile;
		private HandleViewNowPlayingMessages mHandler;
		
		public TrackerThread(JrFile file, HandleViewNowPlayingMessages handler) {
			mFile = file;
			mHandler = handler;
		}
		
		@Override
		public void run() {
			Message msg;
			
			while (true) {
				try {
					
					msg = null;
					if (PollConnectionTask.Instance.get().isRunning()) {
						msg = new Message();
						msg.arg1 = HandleViewNowPlayingMessages.SHOW_CONNECTION_LOST;
					} else if (mFile !=null && mFile.isPlaying()) {
						msg = new Message();
						msg.arg1 = HandleViewNowPlayingMessages.UPDATE_PLAYING;
					}
					if (msg != null) mHandler.sendMessage(msg);
					
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}