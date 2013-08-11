package com.lasthopesoftware.bluewater;

import java.io.FileNotFoundException;
import java.util.Timer;
import java.util.TimerTask;

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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class ViewNowPlaying extends Activity implements Runnable {
	private static Thread mTrackerThread;
	private static HandleStreamMessages mHandler;
	private ImageButton mPlay;
	private ImageButton mPause;
	private ImageButton mNext;
	private ImageButton mPrevious;
	private static FrameLayout mContentView;
	private static RelativeLayout mControlNowPlaying, mViewCoverArt;
	private Timer hideTimer;
	private TimerTask timerTask;

	private static int UPDATE_ALL = 0;
	private static int UPDATE_PLAYING = 1;
	private static int SET_STOPPED = 2;
	private static int HIDE_CONTROLS = 3;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContentView = new FrameLayout(this);
		setContentView(mContentView);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		mViewCoverArt = (RelativeLayout) inflater.inflate(R.layout.activity_view_cover_art, null);
		mControlNowPlaying = (RelativeLayout) inflater.inflate(R.layout.activity_control_now_playing, null);
		
		mContentView.addView(mControlNowPlaying);
		mContentView.addView(mViewCoverArt);
		
		hideTimer = new Timer("Fade Timer");
		
		
		mContentView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mControlNowPlaying.bringToFront();
				mContentView.invalidate();
				if (timerTask != null) timerTask.cancel();
				hideTimer.purge();
				timerTask = new TimerTask() {
					
					@Override
					public void run() {
						Message msg = new Message();
						msg.arg1 = HIDE_CONTROLS;
						mHandler.sendMessage(msg);
					}
				};
				hideTimer.schedule(timerTask, 5000);
			}
		});

		mPlay = (ImageButton) findViewById(R.id.btnPlay);
		mPause = (ImageButton) findViewById(R.id.btnPause);
		mNext = (ImageButton) findViewById(R.id.btnNext);
		mPrevious = (ImageButton) findViewById(R.id.btnPrevious);
		
		/* Toggle play/pause */
		TogglePlayPauseListener togglePlayPauseListener = new TogglePlayPauseListener(mPlay, mPause);
		mPlay.setOnClickListener(togglePlayPauseListener);
		mPause.setOnClickListener(togglePlayPauseListener);

		mNext.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				StreamingMusicService.Next(v.getContext());
			}
		});
		
		mPrevious.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				StreamingMusicService.Previous(v.getContext());
			}
		});
		
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
	
	private static class TogglePlayPauseListener implements OnClickListener {
		private View mPlay, mPause;

		public TogglePlayPauseListener(View play, View pause) {
			mPlay = play;
			mPause = pause;
		}

		@Override
		public void onClick(View v) {
			if (JrSession.playingFile.isPlaying()) {
				StreamingMusicService.Pause(v.getContext());
				mPause.setVisibility(View.INVISIBLE);
				mPlay.setVisibility(View.VISIBLE);
				return;
			}
			
			if (JrSession.playingFile.isPrepared()) StreamingMusicService.Play(v.getContext());
			else StreamingMusicService.StreamMusic(v.getContext(), JrSession.playingFile, JrSession.playlist);
			
			mPlay.setVisibility(View.INVISIBLE);
			mPause.setVisibility(View.VISIBLE);
			
			return;
		}

	}
	
	public FrameLayout getContentView() {
		return mContentView;
	}
	
	public RelativeLayout getControlNowPlaying() {
		return mControlNowPlaying;
	}
	
	public RelativeLayout getViewCoverArt() {
		return mViewCoverArt;
	}
	
	private static class HandleStreamMessages extends Handler {
		private TextView mNowPlayingArtist, mNowPlayingTitle;
		private ImageView mNowPlayingImg;
		private ProgressBar mSongProgress;
		private ImageButton mPlay;
		private ImageButton mPause;
		private ProgressBar mLoadingImg;
		private RatingBar mSongRating;
		private static GetFileImage getFileImageTask;
		private ViewNowPlaying mOwner;

		public HandleStreamMessages(ViewNowPlaying owner) {
			mOwner = owner;
			mSongProgress = (ProgressBar) mOwner.findViewById(R.id.pbNowPlaying);
			mLoadingImg = (ProgressBar) mOwner.findViewById(R.id.pbLoadingImg);
			mNowPlayingImg = (ImageView) mOwner.findViewById(R.id.imgNowPlaying);
			mPlay = (ImageButton) mOwner.findViewById(R.id.btnPlay);
			mPause = (ImageButton) mOwner.findViewById(R.id.btnPause);
			mNowPlayingArtist = (TextView) mOwner.findViewById(R.id.tvSongArtist);
			mNowPlayingTitle = (TextView) mOwner.findViewById(R.id.tvSongTitle);
			mSongRating = (RatingBar) mOwner.findViewById(R.id.rbSongRating);
			setView();
		}

		@Override
		public void handleMessage(Message msg) {
			if (msg.arg1 == SET_STOPPED) {
				mSongProgress.setProgress(0);
			} else if (msg.arg1 == UPDATE_ALL) {
				setView();
			} else if (msg.arg1 == UPDATE_PLAYING) {
				mPause.setVisibility(View.VISIBLE);
				mPlay.setVisibility(View.INVISIBLE);
				if (JrSession.playingFile != null) {
					mSongProgress.setMax(JrSession.playingFile.getMediaPlayer().getDuration());
					mSongProgress.setProgress(JrSession.playingFile.getMediaPlayer().getCurrentPosition());
				}
			} else if (msg.arg1 == HIDE_CONTROLS) {
				mOwner.getViewCoverArt().bringToFront();
				mOwner.getContentView().invalidate();
			}
		}

		private void setView() {
			if (JrSession.playingFile == null) return;
			
//			if (JrSession.playingFile.getProperty("Album") != null) title += "\n (" + JrSession.playingFile.getProperty("Album") + ")";
			mNowPlayingArtist.setText(JrSession.playingFile.getProperty("Artist"));
			mNowPlayingTitle.setText(JrSession.playingFile.getValue());
			if (JrSession.playingFile.getProperty("Rating") != null && !JrSession.playingFile.getProperty("Rating").isEmpty()) {
				mSongRating.setRating(Float.valueOf(JrSession.playingFile.getProperty("Rating")));
				mSongRating.invalidate();
			}
			
			if (JrSession.playingFile.isPrepared()) {
				mSongProgress.setMax(JrSession.playingFile.getMediaPlayer().getDuration());
				mSongProgress.setProgress(JrSession.playingFile.getMediaPlayer().getCurrentPosition());
			}
			try {
				int size = mNowPlayingImg.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? mNowPlayingImg.getHeight() : mNowPlayingImg.getWidth();
				
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
				mNowPlayingImg.setScaleType(ScaleType.CENTER);
				mLoadingImg.setVisibility(View.INVISIBLE);
				mNowPlayingImg.setVisibility(View.VISIBLE);
			}
		}
	}
}
