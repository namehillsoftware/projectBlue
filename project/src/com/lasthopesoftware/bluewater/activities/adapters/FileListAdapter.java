package com.lasthopesoftware.bluewater.activities.adapters;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.ViewFileDetails;
import com.lasthopesoftware.bluewater.activities.listeners.OnSwipeListener;
import com.lasthopesoftware.bluewater.activities.listeners.OnSwipeListener.OnSwipeRightListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.FilePlayer;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.PlaylistController;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingStartListener;
import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.service.objects.Files;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.services.StreamingMusicService;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

public class FileListAdapter extends ArrayAdapter<File> {
	
	private List<File> mFiles;
	
	private static class ViewHolder {
		public ViewHolder(final CharSequence loadingText, final RelativeLayout textLayout, final TextView textView, final ImageButton addButton, final ImageButton playButton, final ImageButton viewFileDetailsButton) {
			this.loadingText = loadingText;
			this.textLayout = textLayout;
			this.textView = textView;
			this.addButton = addButton;
			this.playButton = playButton;
			this.viewFileDetailsButton = viewFileDetailsButton;
		}
		
		final CharSequence loadingText;
		final RelativeLayout textLayout;
		final TextView textView;
		final ImageButton addButton;
		final ImageButton playButton;
		final ImageButton viewFileDetailsButton;
		
		GetFileValueTask getFileValueTask;
		OnNowPlayingStartListener checkIfIsPlayingFileListener;
		OnAttachStateChangeListener onAttachStateChangeListener;
	}
	
	public FileListAdapter(Context context, int resource, List<File> files) {
		super(context, resource, files);
		
		mFiles = files;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		if (convertView == null) {
			convertView = new ViewFlipper(parent.getContext());
		
			final ViewFlipper parentView = (ViewFlipper)convertView;
			parentView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			
			final  OnSwipeListener onSwipeListener = new OnSwipeListener(parentView.getContext());
			onSwipeListener.setOnSwipeRightListener(new OnSwipeRightListener() {
				
				@Override
				public boolean onSwipeRight(View view) {
					parentView.showPrevious();
					return true;
				}
			});
			parentView.setOnTouchListener(onSwipeListener);
			
			final LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final RelativeLayout rl = (RelativeLayout) inflater.inflate(R.layout.layout_standard_text, parentView, false);
			final TextView textView = (TextView) rl.findViewById(R.id.tvStandard);
			
		
			textView.setMarqueeRepeatLimit(1);
			
			parentView.addView(rl);
			
			final LinearLayout fileMenu = (LinearLayout)inflater.inflate(R.layout.layout_file_item_menu, parentView, false);
	        fileMenu.setOnTouchListener(onSwipeListener);
	        
	        final ImageButton addButton = (ImageButton)fileMenu.findViewById(R.id.btnAddToPlaylist);
	        addButton.setOnTouchListener(onSwipeListener);
	        
	        final ImageButton playButton = (ImageButton)fileMenu.findViewById(R.id.btnPlaySong);
	        playButton.setOnTouchListener(onSwipeListener);
	        
	        final ImageButton viewFileDetailsButton = (ImageButton)fileMenu.findViewById(R.id.btnViewFileDetails);
	        viewFileDetailsButton.setOnTouchListener(onSwipeListener);
			
			parentView.addView(fileMenu);
			
			convertView.setTag(new ViewHolder(parent.getContext().getText(R.string.lbl_loading), rl, textView, addButton, playButton, viewFileDetailsButton));
		}
		
		final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        
		viewHolder.textView.setText(viewHolder.loadingText);
        
        final File file = getItem(position);
        
        viewHolder.textView.setTypeface(null, Typeface.NORMAL);
        if (StreamingMusicService.getPlaylistController() == null)
			StreamingMusicService.resumeSavedPlaylist(convertView.getContext());
		
		final PlaylistController playlistController = StreamingMusicService.getPlaylistController();
        if (playlistController != null && playlistController.getCurrentFilePlayer() != null && playlistController.getCurrentFilePlayer().getFile().getKey() == file.getKey())
        	viewHolder.textView.setTypeface(null, Typeface.BOLD);
        
        if (viewHolder.getFileValueTask != null) viewHolder.getFileValueTask.cancel(false);
        viewHolder.getFileValueTask = GetFileValueTask.getFileValue(position, file, (ListView)parent, viewHolder.textView);

		if (viewHolder.checkIfIsPlayingFileListener != null) StreamingMusicService.removeOnStreamingStartListener(viewHolder.checkIfIsPlayingFileListener);
		viewHolder.checkIfIsPlayingFileListener = new OnNowPlayingStartListener() {
				
			@Override
			public void onNowPlayingStart(PlaylistController controller, FilePlayer filePlayer) {
				viewHolder.textView.setTypeface(null, filePlayer.getFile().getKey() == file.getKey() ? Typeface.BOLD : Typeface.NORMAL);
			}
		};
		
		StreamingMusicService.addOnStreamingStartListener(viewHolder.checkIfIsPlayingFileListener);
		
		if (viewHolder.onAttachStateChangeListener != null) viewHolder.textLayout.removeOnAttachStateChangeListener(viewHolder.onAttachStateChangeListener);
		viewHolder.onAttachStateChangeListener = new OnAttachStateChangeListener() {
			
			@Override
			public void onViewDetachedFromWindow(View v) {
				if (viewHolder.checkIfIsPlayingFileListener != null)
					StreamingMusicService.removeOnStreamingStartListener(viewHolder.checkIfIsPlayingFileListener);
			}
			
			@Override
			public void onViewAttachedToWindow(View v) {
				return;
			}
		};
		
		viewHolder.textLayout.addOnAttachStateChangeListener(viewHolder.onAttachStateChangeListener);
		
		viewHolder.viewFileDetailsButton.setOnClickListener(new ViewFileDetailsClickHandler(file));
		viewHolder.addButton.setOnClickListener(new AddClickHandler(file));
		viewHolder.playButton.setOnClickListener(new PlayClickHandler(position, mFiles));
		
		return convertView;
	}
	
	private static class PlayClickHandler implements OnClickListener {
		private List<File> mFiles;
		private int mPosition;
		
		public PlayClickHandler(int position, List<File> files) {
			mPosition = position;
			mFiles = files;
		}
		
		@Override
		public void onClick(View v) {
			StreamingMusicService.streamMusic(v.getContext(), mPosition, Files.serializeFileStringList(mFiles));
		}
	}
	
	private static class AddClickHandler implements OnClickListener {
		private File mFile;
		
		public AddClickHandler(File file) {
			mFile = file;
		}
		
		@Override
		public void onClick(View v) {
			final Context _context = v.getContext();
			if (StreamingMusicService.getPlaylistController() == null) 
				StreamingMusicService.resumeSavedPlaylist(_context);
			
			StreamingMusicService.getPlaylistController().addFile(mFile);
			
			JrSession.GetLibrary(_context, new OnCompleteListener<Integer, Void, Library>() {

				@Override
				public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
					if (result == null) return;
					String newFileString = result.getSavedTracksString();
					if (!newFileString.endsWith(";")) newFileString += ";";
					newFileString += mFile.getKey() + ";";
					result.setSavedTracksString(newFileString);
					
					Toast.makeText(_context, _context.getText(R.string.lbl_song_added_to_now_playing), Toast.LENGTH_SHORT).show();;
				}
				
			});
			
		}
	}
	
	private static class ViewFileDetailsClickHandler implements OnClickListener {
		private File mFile;
		
		public ViewFileDetailsClickHandler(File file) {
			mFile = file;
		}
		
		@Override
		public void onClick(View v) {
    		Intent intent = new Intent(v.getContext(), ViewFileDetails.class);
    		intent.putExtra(ViewFileDetails.FILE_KEY, mFile.getKey());
    		v.getContext().startActivity(intent);
		}
	}
	
	private static class GetFileValueTask extends AsyncTask<String, Void, String> {
		private int mPosition;
		private ListView mParentListView;
		private TextView mTextView;
		private File mFile;
		
		public static GetFileValueTask getFileValue(int position, File file, ListView parentListView, TextView textView) {
			return (GetFileValueTask) (new GetFileValueTask(position, file, parentListView, textView)).execute();
		}
		
		private GetFileValueTask(int position, File file, ListView parentListView, TextView textView) {
			mPosition = position;
			mParentListView = parentListView;
			mFile = file;
			mTextView = textView;
		}

		@Override
		protected String doInBackground(String... params) {
			if (isCancelled() || (mPosition < mParentListView.getFirstVisiblePosition() - 10) || (mPosition > mParentListView.getLastVisiblePosition() + 10)) return null;
			return mFile.getValue();
		}
		
		@Override
		protected void onPostExecute(String result) {
			if (result != null)
				mTextView.setText(result);
		}
	}
}