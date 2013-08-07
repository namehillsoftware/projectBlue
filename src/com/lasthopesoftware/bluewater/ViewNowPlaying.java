package com.lasthopesoftware.bluewater;

import java.io.FileNotFoundException;

import jrAccess.JrConnection;
import jrAccess.JrSession;
import jrFileSystem.JrFile;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;

public class ViewNowPlaying extends Activity implements Runnable {
	private static Thread mTrackerThread;
	private static HandleStreamMessages mHandler;

	private static int UPDATE_ALL = 0;
	private static int UPDATE_PLAYING = 1;
	private static int SET_STOPPED = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		RelativeLayout layout = new RelativeLayout(this);
//		
//		View nowPlayingView = getLayoutInflater().inflate(R.layout.activity_view_now_playing, layout);
//		View coverArtView = getLayoutInflater().inflate(R.layout.activity_ctl_now_playing, layout);
//		layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
////		layout.addView(nowPlayingView);
//		layout.addView(coverArtView);
//		nowPlayingView.bringToFront();
//		setContentView(layout);
		setContentView(R.layout.activity_view_now_playing);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);

		mHandler = new HandleStreamMessages(this);
		if (mTrackerThread != null) mTrackerThread.interrupt();

		mTrackerThread = new Thread(this);
		mTrackerThread.setPriority(Thread.MIN_PRIORITY);
		mTrackerThread.setName("Tracker Thread");
		mTrackerThread.start();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_blue_water, menu);
		menu.findItem(R.id.menu_view_now_playing).setVisible(ViewUtils.displayNowPlayingMenu());
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (ViewUtils.handleNavMenuClicks(this, item)) return true;
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void run() {
		JrFile playingFile = null;
		Message msg;
		try {
			while (JrSession.playlist != null && !JrSession.playlist.isEmpty()) {
				msg = null;

				if (JrSession.playingFile == null || JrSession.playingFile.getMediaPlayer() == null) {
					playingFile = null;
					msg = new Message();
					msg.arg1 = SET_STOPPED;
				} else if ((playingFile == null && JrSession.playingFile != null) || !playingFile.equals(JrSession.playingFile)) {
					playingFile = JrSession.playingFile;
					msg = new Message();
					msg.arg1 = UPDATE_ALL;
				} else if (JrSession.playingFile.isPlaying()) {
					msg = new Message();
					msg.arg1 = UPDATE_PLAYING;
				}
				if (msg != null) mHandler.sendMessage(msg);
				Thread.sleep(1000);

			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
	}

	private static class HandleStreamMessages extends Handler {
		private ImageView mNowPlayingImg;
		private ProgressBar mLoadingImg;
		private static GetFileImage getFileImageTask;

		public HandleStreamMessages(ViewNowPlaying owner) {
			mLoadingImg = (ProgressBar) owner.findViewById(R.id.pbLoadingImg);
			mNowPlayingImg = (ImageView) owner.findViewById(R.id.imgNowPlaying);
			setView();
//			mNext = (ImageButton) findViewById(R.id.btnNext);
//			mPrevious = (ImageButton) findViewById(R.id.btnPrevious);
		}

		@Override
		public void handleMessage(Message msg) {
			
		}

		private void setView() {
			if (JrSession.playingFile == null) return;
			
			try {
				int size = mNowPlayingImg.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? mNowPlayingImg.getWidth() : mNowPlayingImg.getHeight();
				
				// Cancel the getFileImageTask if it is already in progress
				if (getFileImageTask != null && (getFileImageTask.getStatus() == AsyncTask.Status.PENDING || getFileImageTask.getStatus() == AsyncTask.Status.RUNNING)) {
					getFileImageTask.cancel(true);
				}
				
				getFileImageTask = new GetFileImage();
				getFileImageTask.execute(JrSession.playingFile.getKey().toString(), String.valueOf(size));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private class GetFileImage extends AsyncTask<String, Void, Bitmap> {
			private boolean isFileFound = false;
			
			@Override
			protected void onPreExecute() {
				mNowPlayingImg.setVisibility(View.INVISIBLE);
				mLoadingImg.setVisibility(View.VISIBLE);
			}
			
			@Override
			protected Bitmap doInBackground(String... params) {

				Bitmap returnBmp = null;

				try {
					JrConnection conn = new JrConnection(
												"File/GetImage", 
												"File=" + params[0], 
												"Type=Full",
												"Width=" + params[1], 
												"Height=" + params[1], 
												"Pad=1",
												"Format=png",
												"FillTransparency=ffffff");
					returnBmp = BitmapFactory.decodeStream(conn.getInputStream());
					isFileFound = true;
				} catch (FileNotFoundException fe) {
					isFileFound = false;
				} catch (Exception e) {
					e.printStackTrace();
				}

				return returnBmp;
			}
			
			@Override
			protected void onPostExecute(Bitmap result) {
				mNowPlayingImg.setImageBitmap(result);
				mNowPlayingImg.setScaleType(ScaleType.CENTER_CROP);
				mLoadingImg.setVisibility(View.INVISIBLE);
				mNowPlayingImg.setVisibility(View.VISIBLE);
			}
		}
	}
}
