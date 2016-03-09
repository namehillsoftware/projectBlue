package com.lasthopesoftware.bluewater.servers.library.items.media.files.menu;

import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.vedsoft.fluent.AsyncExceptionTask;

import java.io.FileNotFoundException;
import java.util.concurrent.ExecutionException;

/**
 * Created by david on 4/14/15.
 */
public class GetFileListItemTextTask extends AsyncExceptionTask<Void, Void, String> {

    private final IFile file;
    private final TextView textView;

    public GetFileListItemTextTask(IFile file, TextView textView) {
        this.file = file;
        this.textView = textView;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

	    if (!isCancelled())
            textView.setText(R.string.lbl_loading);
    }

    @Override
    protected String doInBackground(Void... params) {
        if (isCancelled()) return null;

        final CachedFilePropertiesProvider filePropertiesProvider = new CachedFilePropertiesProvider(SessionConnection.getSessionConnectionProvider(), file.getKey());
        filePropertiesProvider.execute();
        try {
            return !isCancelled() ? filePropertiesProvider.get().get(FilePropertiesProvider.NAME) : null;
        } catch (ExecutionException | InterruptedException e) {
	        setException(e);
        }
	    return null;
    }

    @Override
    protected void onPostExecute(String s, Exception exception) {
        super.onPostExecute(s);

	    if (exception instanceof FileNotFoundException) {
		    textView.setText(R.string.file_not_found);
		    return;
	    }

        if (s != null && !isCancelled()) textView.setText(s);
    }
}
