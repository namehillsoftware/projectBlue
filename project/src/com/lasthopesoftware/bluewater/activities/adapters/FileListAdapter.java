package com.lasthopesoftware.bluewater.activities.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrFilePlayer;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrPlaylistController;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingStartListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingStopListener;
import com.lasthopesoftware.bluewater.data.service.objects.JrFile;
import com.lasthopesoftware.bluewater.services.StreamingMusicService;

public class FileListAdapter extends BaseAdapter {
	private ArrayList<JrFile> mFiles;
	
	public FileListAdapter(ArrayList<JrFile> files) {
		mFiles = files;
	}
	
	@Override
	public int getCount() {
		return mFiles.size();
	}

	@Override
	public Object getItem(int position) {
		return mFiles.get(position);
	}

	@Override
	public long getItemId(int position) {
		return mFiles.get(position).getKey();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	
		final LayoutInflater inflator = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final RelativeLayout returnView = (RelativeLayout) inflator.inflate(R.layout.layout_file_item, null);
		final TextView textView = (TextView) returnView.findViewById(R.id.tvSongName);
		final ImageView imgIsPlaying = (ImageView) returnView.findViewById(R.id.imgIsPlaying);
		final JrFile file = mFiles.get(position);
        textView.setMarqueeRepeatLimit(1);
        textView.setText("Loading...");
        GetFileValueTask.getFileValue(position, file, (ListView)parent, textView);
        
        final OnNowPlayingStartListener checkIfIsPlayingFileListener = new OnNowPlayingStartListener() {
			
			@Override
			public void onNowPlayingStart(JrPlaylistController controller, JrFilePlayer filePlayer) {
				imgIsPlaying.setVisibility(filePlayer.getFile().getKey().equals(file.getKey()) ? View.VISIBLE : View.INVISIBLE);
			}
		};
		
		final OnNowPlayingStopListener onNowPlayingStop = new OnNowPlayingStopListener() {
			
			@Override
			public void onNowPlayingStop(JrPlaylistController controller, JrFilePlayer filePlayer) {
				imgIsPlaying.setVisibility(View.INVISIBLE);
			}
		};
		
        returnView.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
			
			@Override
			public void onViewDetachedFromWindow(View v) {
				StreamingMusicService.removeOnStreamingStartListener(checkIfIsPlayingFileListener);
				StreamingMusicService.removeOnStreamingStopListener(onNowPlayingStop);
			}
			
			@Override
			public void onViewAttachedToWindow(View v) {
				final JrPlaylistController playlistController = StreamingMusicService.getPlaylistController();
		        if (playlistController != null && playlistController.getCurrentFilePlayer() != null && playlistController.getCurrentFilePlayer().getFile().getKey().equals(file.getKey()))
		        	imgIsPlaying.setVisibility(View.VISIBLE);
		        
				StreamingMusicService.addOnStreamingStartListener(checkIfIsPlayingFileListener);
				StreamingMusicService.addOnStreamingStopListener(onNowPlayingStop);
			}
		});
        
		return returnView;
	}
	
	private static class GetFileValueTask extends AsyncTask<String, Void, String> {
		private int mPosition;
		private ListView mParentListView;
		private TextView mTextView;
		private JrFile mFile;
		
		public static GetFileValueTask getFileValue(int position, JrFile file, ListView parentListView, TextView textView) {
			return (GetFileValueTask) (new GetFileValueTask(position, file, parentListView, textView)).execute();
		}
		
		private GetFileValueTask(int position, JrFile file, ListView parentListView, TextView textView) {
			mPosition = position;
			mParentListView = parentListView;
			mFile = file;
			mTextView = textView;
		}

		@Override
		protected String doInBackground(String... params) {
			if ((mPosition < mParentListView.getFirstVisiblePosition() - 10) || (mPosition > mParentListView.getLastVisiblePosition() + 10)) return null;
			return mFile.getValue();
		}
		
		@Override
		protected void onPostExecute(String result) {
			if (result == null) return;
			
			mTextView.setText(result);
		}
	}
}