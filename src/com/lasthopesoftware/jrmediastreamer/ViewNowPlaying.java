package com.lasthopesoftware.jrmediastreamer;

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
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

public class ViewNowPlaying extends Activity implements Runnable {
	private TextView mNowPlayingText;
	private ImageView mNowPlayingImg;
	private ProgressBar mLoadingImg;
	private SeekBar mSeekbar;
	private ImageButton mPlay;
	private ImageButton mPause;
	private ImageButton mNext;
	private ImageButton mPrevious;
	private static Thread mTrackerThread;
	private static HandleStreamMessages mHandler;

	private static int UPDATE_ALL = 0;
	private static int UPDATE_PLAYING = 1;
	private static int SET_STOPPED = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_now_playing);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		mSeekbar = (SeekBar) findViewById(R.id.sbNowPlaying);
		mNowPlayingImg = (ImageView) findViewById(R.id.imgNowPlaying);
		mNowPlayingText = (TextView) findViewById(R.id.tvNowPlaying);
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
		mHandler = new HandleStreamMessages(mNowPlayingText, mNowPlayingImg, mSeekbar, mPlay, mPause);
		if (mTrackerThread != null) mTrackerThread.interrupt();

		mTrackerThread = new Thread(this);
		mTrackerThread.setPriority(Thread.MIN_PRIORITY);
		mTrackerThread.setName("Tracker Thread");
		mTrackerThread.start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_view_now_playing, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
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
			} else {
				StreamingMusicService.Play(v.getContext());
				mPlay.setVisibility(View.INVISIBLE);
				mPause.setVisibility(View.VISIBLE);
			}
		}

	}

	@Override
	public void run() {
		JrFile playingFile = null;
		Message msg;
		try {
			while (JrSession.playlist != null && !JrSession.playlist.isEmpty()) {
				msg = null;

				if (JrSession.playingFile == null) {
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
		private TextView mNowPlayingText;
		private ImageView mNowPlayingImg;
		private SeekBar mSeekbar;
		private ImageButton mPlay;
		private ImageButton mPause;

		public HandleStreamMessages(TextView tv, ImageView iv, SeekBar seekbar, ImageButton play, ImageButton pause) {
			mNowPlayingText = tv;
			mNowPlayingImg = iv;
			mSeekbar = seekbar;
			mPlay = play;
			mPause = pause;
		}

		@Override
		public void handleMessage(Message msg) {
			if (msg.arg1 == SET_STOPPED) {
				mSeekbar.setProgress(0);
			} else if (msg.arg1 == UPDATE_ALL) {
				setView();
			} else if (msg.arg1 == UPDATE_PLAYING) {
				mPause.setVisibility(View.VISIBLE);
				mPlay.setVisibility(View.INVISIBLE);
				if (JrSession.playingFile != null) {
					mSeekbar.setMax(JrSession.playingFile.getMediaPlayer().getDuration());
					mSeekbar.setProgress(JrSession.playingFile.getMediaPlayer().getCurrentPosition());
				}
			}
		}

		private void setView() {
			String title = JrSession.playingFile.getProperty("Artist") + " - " + JrSession.playingFile.getValue();
			if (JrSession.playingFile.getProperty("Album") != null) title += "\n (" + JrSession.playingFile.getProperty("Album") + ")";

			mNowPlayingText.setText(title);
			try {
				int size = mNowPlayingImg.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? mNowPlayingImg.getWidth() : mNowPlayingImg.getHeight(); 
				mNowPlayingImg.setImageBitmap(new GetFileImage().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, JrSession.playingFile.getKey().toString(), String.valueOf(size)).get());
				mNowPlayingImg.setScaleType(ScaleType.CENTER_CROP);
			} catch (Exception e) {
				e.printStackTrace();
			}

			mSeekbar.setMax(JrSession.playingFile.getMediaPlayer().getDuration());
			mSeekbar.setProgress(JrSession.playingFile.getMediaPlayer().getCurrentPosition());
		}

		private class GetFileImage extends AsyncTask<String, Void, Bitmap> {
			@Override
			protected void onPreExecute() {
				mNowPlayingImg.setVisibility(View.INVISIBLE);
				
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
				} catch (Exception e) {
					e.printStackTrace();
				}

				return returnBmp;
			}
		}
	}
}
