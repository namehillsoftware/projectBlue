package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.activity.adapter;

import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.activity.adapter.viewholder.ActiveFileDownloadsViewHolder;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.repository.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

import java.util.List;

/**
 * Created by david on 8/23/15.
 */
public class ActiveFileDownloadsAdapter extends RecyclerView.Adapter<ActiveFileDownloadsViewHolder> {

	private final ConnectionProvider connectionProvider;
	private final List<StoredFile> downloadingFiles;

	public ActiveFileDownloadsAdapter(ConnectionProvider connectionProvider, List<StoredFile> downloadingFiles) {
		this.connectionProvider = connectionProvider;
		this.downloadingFiles = downloadingFiles;
	}

	@Override
	public ActiveFileDownloadsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		final TextView textView =
				(TextView)LayoutInflater
						.from(parent.getContext())
						.inflate(R.layout.layout_standard_text, parent, false);

		return new ActiveFileDownloadsViewHolder(textView);
	}

	@Override
	public void onBindViewHolder(final ActiveFileDownloadsViewHolder holder, final int position) {
		holder.textView.setText(R.string.lbl_loading);

		final SimpleTask<Void, Void, String> getFileName = new SimpleTask<>(new ISimpleTask.OnExecuteListener<Void, Void, String>() {
			@Override
			public String onExecute(ISimpleTask<Void, Void, String> owner, Void... params) throws Exception {
				final FilePropertiesProvider filePropertiesProvider = new FilePropertiesProvider(connectionProvider, downloadingFiles.get(position).getServiceId());
				return filePropertiesProvider.getProperty(FilePropertiesProvider.NAME);
			}
		});

		getFileName.addOnCompleteListener(new ISimpleTask.OnCompleteListener<Void, Void, String>() {
			@Override
			public void onComplete(ISimpleTask<Void, Void, String> owner, String s) {
				holder.textView.setText(s);
			}
		});

		getFileName.execute(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public int getItemCount() {
		return downloadingFiles.size();
	}
}
