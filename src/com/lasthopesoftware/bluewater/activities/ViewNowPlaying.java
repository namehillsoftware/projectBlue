package com.lasthopesoftware.bluewater.activities;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.util.LruCache;
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
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.ViewNowPlayingHelpers.HandleViewNowPlayingMessages;
import com.lasthopesoftware.bluewater.activities.ViewNowPlayingHelpers.TrackerThread;
import com.lasthopesoftware.bluewater.activities.common.ViewUtils;
import com.lasthopesoftware.bluewater.data.access.connection.JrConnection;
import com.lasthopesoftware.bluewater.data.access.connection.PollConnectionTask;
import com.lasthopesoftware.bluewater.data.objects.JrFile;
import com.lasthopesoftware.bluewater.data.objects.JrSession;
import com.lasthopesoftware.bluewater.services.OnStreamingStartListener;
import com.lasthopesoftware.bluewater.services.OnStreamingStopListener;
import com.lasthopesoftware.bluewater.services.StreamingMusicService;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

public class ViewNowPlaying extends Activity implements OnStreamingStartListener, OnStreamingStopListener {
	private static Thread mTrackerThread;
	private static HandleViewNowPlayingMessages mHandler;
	private ImageButton mPlay;
	private ImageButton mPause;
	private ImageButton mNext;
	private ImageButton mPrevious;
	private RatingBar mSongRating;
	private static FrameLayout mContentView;
	private static RelativeLayout mControlNowPlaying, mViewCoverArt;
	private Timer mHideTimer;
	private TimerTask mTimerTask;
	
	private ProgressBar mSongProgress;
	private ProgressBar mLoadingImg;
	private ImageView mNowPlayingImg;
	private TextView mNowPlayingArtist;
	private TextView mNowPlayingTitle;
	private static GetFileImage getFileImageTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (!JrSession.Active) JrSession.CreateSession(this);
		
		mContentView = new FrameLayout(this);
		setContentView(mContentView);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		mViewCoverArt = (RelativeLayout) inflater.inflate(R.layout.activity_view_cover_art, null);
		mControlNowPlaying = (RelativeLayout) inflater.inflate(R.layout.activity_control_now_playing, null);
		mControlNowPlaying.setVisibility(View.INVISIBLE);
		mContentView.addView(mViewCoverArt);
		mContentView.addView(mControlNowPlaying);
		
		mHideTimer = new Timer("Fade Timer");
		
		
		mContentView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mControlNowPlaying.setVisibility(View.VISIBLE);
				mContentView.invalidate();
				if (mTimerTask != null) mTimerTask.cancel();
				mHideTimer.purge();
				mTimerTask = new TimerTask() {
					
					@Override
					public void run() {
						Message msg = new Message();
						msg.arg1 = HandleViewNowPlayingMessages.HIDE_CONTROLS;
						mHandler.sendMessage(msg);
					}
				};
				mHideTimer.schedule(mTimerTask, 5000);
			}
		});

		mPlay = (ImageButton) findViewById(R.id.btnPlay);
		mPause = (ImageButton) findViewById(R.id.btnPause);
		mNext = (ImageButton) findViewById(R.id.btnNext);
		mPrevious = (ImageButton) findViewById(R.id.btnPrevious);
		mSongRating = (RatingBar) findViewById(R.id.rbSongRating);
		mSongProgress = (ProgressBar) findViewById(R.id.pbNowPlaying);
		mLoadingImg = (ProgressBar) findViewById(R.id.pbLoadingImg);
		mNowPlayingImg = (ImageView) findViewById(R.id.imgNowPlaying);
		mNowPlayingArtist = (TextView) findViewById(R.id.tvSongArtist);
		mNowPlayingTitle = (TextView) findViewById(R.id.tvSongTitle);
		
		StreamingMusicService.AddOnStreamingStartListener(this);
		StreamingMusicService.AddOnStreamingStopListener(this);
		
		/* Toggle play/pause */
		TogglePlayPauseListener togglePlayPauseListener = new TogglePlayPauseListener();
		mPlay.setOnClickListener(togglePlayPauseListener);
		mPause.setOnClickListener(togglePlayPauseListener);

		mNext.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!mControlNowPlaying.isShown()) return;
				StreamingMusicService.Next(v.getContext());
			}
		});
		
		mPrevious.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!mControlNowPlaying.isShown()) return;
				StreamingMusicService.Previous(v.getContext());
			}
		});
		
		mSongRating.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
			
			@Override
			public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
				if (!fromUser || !mControlNowPlaying.isShown()) return;
				JrSession.PlayingFile.setProperty("Rating", String.valueOf(Math.round(rating)));
			}
		});
		
		mHandler = new HandleViewNowPlayingMessages(this);
		
		if (JrSession.PlayingFile == null) return; 
		
		if (mTrackerThread != null && mTrackerThread.isAlive()) mTrackerThread.interrupt();

		mTrackerThread = new Thread(new TrackerThread(JrSession.PlayingFile, mHandler));
		mTrackerThread.setPriority(Thread.MIN_PRIORITY);
		mTrackerThread.setName("Tracker Thread");
		mTrackerThread.start();
		
		setView(JrSession.PlayingFile);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
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
	
	private static class TogglePlayPauseListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			if (!mControlNowPlaying.isShown()) return;
			
			if (JrSession.PlayingFile.isPlaying()) {
				StreamingMusicService.Pause(v.getContext());
				return;
			}
						
			if (JrSession.PlayingFile.isPrepared()) StreamingMusicService.Play(v.getContext());
			else StreamingMusicService.StreamMusic(v.getContext(), JrSession.PlayingFile.getKey(), JrSession.PlayingFile.getCurrentPosition(), JrSession.Playlist);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mHideTimer != null) {
			mHideTimer.cancel();
			mHideTimer.purge();
		}
		
		StreamingMusicService.RemoveOnStreamingStartListener(this);
		StreamingMusicService.RemoveOnStreamingStopListener(this);
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
	
	private void setView(JrFile file) {
		final JrFile playingFile = file;
		
		String artist = "";
		String album = "";
		
		try {
			artist = playingFile.getProperty("Artist");
			album = playingFile.getProperty("Album");
			
			mSongProgress.setMax(playingFile.getDuration());
			mSongRating.setRating(0);
			if (playingFile.getProperty("Rating") != null && !playingFile.getProperty("Rating").isEmpty()) {
				mSongRating.setRating(Float.valueOf(playingFile.getProperty("Rating")));
				mSongRating.invalidate();
			}
			mSongProgress.setProgress(playingFile.getCurrentPosition());
		} catch (IOException ioE) {
			PollConnectionTask.Instance.get().addOnCompleteListener(new OnCompleteListener<String, Void, Boolean>() {
				
				@Override
				public void onComplete(ISimpleTask<String, Void, Boolean> owner, Boolean result) {
					if (result == Boolean.TRUE) setView(playingFile);
				}
			});
			WaitForConnectionDialog.show(this);
		}
		
		mNowPlayingArtist.setText(artist);
		mNowPlayingTitle.setText(playingFile.getValue());

		try {
			int size = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? getResources().getDisplayMetrics().heightPixels : getResources().getDisplayMetrics().widthPixels;
			
			// Cancel the getFileImageTask if it is already in progress
			if (getFileImageTask != null && (getFileImageTask.getStatus() == AsyncTask.Status.PENDING || getFileImageTask.getStatus() == AsyncTask.Status.RUNNING)) {
				getFileImageTask.cancel(true);
			}
			
			getFileImageTask = new GetFileImage(mNowPlayingImg, mLoadingImg);
			
			getFileImageTask.execute(album == null ? playingFile.getKey().toString() : (artist + ":" + album), playingFile.getKey().toString(), String.valueOf(size));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		mPlay.setVisibility(!file.isPlaying() ? View.VISIBLE : View.INVISIBLE);
		mPause.setVisibility(file.isPlaying() ? View.VISIBLE : View.INVISIBLE);
	}
	
	private static class GetFileImage extends AsyncTask<String, Void, Bitmap> {
		private boolean isFileFound = false;
		private ImageView mNowPlayingImg;
		private ProgressBar mLoadingImg;
		
		private final int cacheSize = 5;			
		private static LruCache<String, Bitmap> imageCache;
		
		private static Bitmap emptyBitmap;

		public GetFileImage(ImageView nowPlayingImg, ProgressBar loadingImg) {
			super();
			mNowPlayingImg = nowPlayingImg;
			mLoadingImg = loadingImg;
			if (imageCache == null) imageCache = new LruCache<String, Bitmap>(cacheSize);
		}
		
		@Override
		protected void onPreExecute() {
			mNowPlayingImg.setVisibility(View.INVISIBLE);
			mLoadingImg.setVisibility(View.VISIBLE);
		}
		
		@Override
		protected Bitmap doInBackground(String... params) {
			
			Bitmap returnBmp = null;
			String uId = params[0];
			String fileKey = params[1];
			String squareSize = params[2];
			
			if (imageCache.get(uId) != null) {
				isFileFound = true;
				return imageCache.get(uId);
			}
			
			try {
				JrConnection conn = new JrConnection(
											"File/GetImage", 
											"File=" + fileKey, 
											"Type=Full",
//											"Width=" + squareSize, 
//											"Height=" + squareSize, 
											"Pad=1",
											"Format=png",
											"FillTransparency=ffffff");
				if (isCancelled()) return null;
				try {
				returnBmp = BitmapFactory.decodeStream(conn.getInputStream());
				isFileFound = true;
				} finally {
					conn.disconnect();
				}
			} catch (FileNotFoundException fe) {
				isFileFound = false;
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (returnBmp == null) {
				if (emptyBitmap == null) {
					int squareInt = Integer.parseInt(squareSize);
					emptyBitmap = Bitmap.createBitmap(squareInt, squareInt, Bitmap.Config.ARGB_8888);
				}
				
				returnBmp = emptyBitmap;
			}
			
			imageCache.put(uId, returnBmp);				
			
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
	

	@Override
	public void onStreamingStart(StreamingMusicService service, JrFile file) {		
		setView(file);
		mPause.setVisibility(View.VISIBLE);
		mPlay.setVisibility(View.INVISIBLE);
		
		if (mTrackerThread != null && mTrackerThread.isAlive()) mTrackerThread.interrupt();

		mTrackerThread = new Thread(new TrackerThread(file, mHandler));
		mTrackerThread.setPriority(Thread.MIN_PRIORITY);
		mTrackerThread.setName("Tracker Thread");
		mTrackerThread.start();
	}
	
	@Override
	public void onStreamingStop(StreamingMusicService service, JrFile file) {
		mTrackerThread.interrupt();
		
		mPlay.setVisibility(View.VISIBLE);
		mPause.setVisibility(View.INVISIBLE);
	}
}
