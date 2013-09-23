package com.lasthopesoftware.bluewater.activities;

import java.io.FileNotFoundException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

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
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.common.ViewUtils;
import com.lasthopesoftware.bluewater.data.access.JrConnection;
import com.lasthopesoftware.bluewater.data.access.JrSession;
import com.lasthopesoftware.bluewater.data.objects.JrFile;
import com.lasthopesoftware.bluewater.services.StreamingMusicService;

public class ViewNowPlaying extends Activity implements Runnable {
	private static Thread mTrackerThread;
	private static HandleStreamMessages mHandler;
	private ImageButton mPlay;
	private ImageButton mPause;
	private ImageButton mNext;
	private ImageButton mPrevious;
	private RatingBar mSongRating;
	private static FrameLayout mContentView;
	private static RelativeLayout mControlNowPlaying, mViewCoverArt;
	private Timer mHideTimer;
	private TimerTask mTimerTask;

	private static int UPDATE_ALL = 0;
	private static int UPDATE_PLAYING = 1;
	private static int SET_STOPPED = 2;
	private static int HIDE_CONTROLS = 3;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

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
						msg.arg1 = HIDE_CONTROLS;
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
		
		/* Toggle play/pause */
		TogglePlayPauseListener togglePlayPauseListener = new TogglePlayPauseListener(mPlay, mPause);
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
		
		mHandler = new HandleStreamMessages(this);
		if (mTrackerThread != null) mTrackerThread.interrupt();

		mTrackerThread = new Thread(this);
		mTrackerThread.setPriority(Thread.MIN_PRIORITY);
		mTrackerThread.setName("Tracker Thread");
		mTrackerThread.start();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Message msg = new Message();
		msg.arg1 = UPDATE_ALL;
		mHandler.sendMessage(msg);
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
			while (JrSession.Playlist != null && !JrSession.Playlist.isEmpty()) {
				msg = null;

				if (JrSession.PlayingFile == null || JrSession.PlayingFile.getMediaPlayer() == null) {
					playingFile = null;
					msg = new Message();
					msg.arg1 = SET_STOPPED;
				} else if ((playingFile == null && JrSession.PlayingFile != null) || !playingFile.equals(JrSession.PlayingFile)) {
					playingFile = JrSession.PlayingFile;
					msg = new Message();
					msg.arg1 = UPDATE_ALL;
				} else if (JrSession.PlayingFile.isPlaying()) {
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
			if (!mControlNowPlaying.isShown()) return;
			
			if (JrSession.PlayingFile.isPlaying()) {
				StreamingMusicService.Pause(v.getContext());
				mPause.setVisibility(View.INVISIBLE);
				mPlay.setVisibility(View.VISIBLE);
				return;
			}
			
			if (JrSession.PlayingFile.isPrepared()) StreamingMusicService.Play(v.getContext());
			else StreamingMusicService.StreamMusic(v.getContext(), JrSession.PlayingFile, JrSession.Playlist);
			
			mPlay.setVisibility(View.INVISIBLE);
			mPause.setVisibility(View.VISIBLE);
			
			return;
		}

	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mHideTimer != null) {
			mHideTimer.cancel();
			mHideTimer.purge();
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
				if (JrSession.PlayingFile != null) {
					mSongProgress.setMax(JrSession.PlayingFile.getMediaPlayer().getDuration());
					mSongProgress.setProgress(JrSession.PlayingFile.getMediaPlayer().getCurrentPosition());
				}
			} else if (msg.arg1 == HIDE_CONTROLS) {
				mOwner.getControlNowPlaying().setVisibility(View.INVISIBLE);
				mOwner.getContentView().invalidate();
			}
		}

		private void setView() {
			if (JrSession.PlayingFile == null) return;
			
			String artist = JrSession.PlayingFile.getProperty("Artist");
			String album = JrSession.PlayingFile.getProperty("Album"); 
			mNowPlayingArtist.setText(artist);
			mNowPlayingTitle.setText(JrSession.PlayingFile.getValue());
			if (JrSession.PlayingFile.getProperty("Rating") != null && !JrSession.PlayingFile.getProperty("Rating").isEmpty()) {
				mSongRating.setRating(Float.valueOf(JrSession.PlayingFile.getProperty("Rating")));
				mSongRating.invalidate();
			}
			
			if (JrSession.PlayingFile.isPrepared()) {
				mSongProgress.setMax(JrSession.PlayingFile.getMediaPlayer().getDuration());
				mSongProgress.setProgress(JrSession.PlayingFile.getMediaPlayer().getCurrentPosition());
			}
			try {
				int size = mOwner.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? mOwner.getResources().getDisplayMetrics().heightPixels : mOwner.getResources().getDisplayMetrics().widthPixels;
				
				// Cancel the getFileImageTask if it is already in progress
				if (getFileImageTask != null && (getFileImageTask.getStatus() == AsyncTask.Status.PENDING || getFileImageTask.getStatus() == AsyncTask.Status.RUNNING)) {
					getFileImageTask.cancel(true);
				}
				
				getFileImageTask = new GetFileImage(mNowPlayingImg, mLoadingImg);
				
				getFileImageTask.execute(album == null ? JrSession.PlayingFile.getKey().toString() : (artist + ":" + album), JrSession.PlayingFile.getKey().toString(), String.valueOf(size));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private static class GetFileImage extends AsyncTask<String, Void, Bitmap> {
			private boolean isFileFound = false;
			private ImageView mNowPlayingImg;
			private ProgressBar mLoadingImg;
			
			private final int cacheSize = 5;			
			private static ConcurrentHashMap<String, Bitmap> imageCache;
			private static ArrayBlockingQueue<String> imageQueue;
			
			private static Bitmap emptyBitmap;
						
			public GetFileImage(ImageView nowPlayingImg, ProgressBar loadingImg) {
				super();
				mNowPlayingImg = nowPlayingImg;
				mLoadingImg = loadingImg;
				if (imageCache == null) imageCache = new ConcurrentHashMap<String, Bitmap>(cacheSize);
				if (imageQueue == null) imageQueue = new ArrayBlockingQueue<String>(cacheSize);
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
				
				if (imageCache.containsKey(uId)) {
					isFileFound = true;
					return imageCache.get(uId);
				}
				
				try {
					JrConnection conn = new JrConnection(
												"File/GetImage", 
												"File=" + fileKey, 
												"Type=Full",
												"Width=" + squareSize, 
												"Height=" + squareSize, 
												"Pad=1",
												"Format=png",
												"FillTransparency=ffffff");
					if (isCancelled()) return null;
					returnBmp = BitmapFactory.decodeStream(conn.getInputStream());
					
					isFileFound = true;
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
				
				while (imageQueue.size() >= cacheSize) {
					String removeFileName;
					try {
						removeFileName = imageQueue.take();
						if (imageCache.containsKey(removeFileName)) imageCache.remove(removeFileName);
					} catch (InterruptedException e) {
						e.printStackTrace();					
						break;
					}
				}
				
				if (!imageCache.containsKey(uId)) {
					imageQueue.add(uId);
					imageCache.put(uId, returnBmp);
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
