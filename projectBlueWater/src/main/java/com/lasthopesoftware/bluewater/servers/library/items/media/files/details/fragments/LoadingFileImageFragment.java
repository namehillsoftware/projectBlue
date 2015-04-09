package com.lasthopesoftware.bluewater.servers.library.items.media.files.details.fragments;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.lasthopesoftware.bluewater.R;


/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class LoadingFileImageFragment extends Fragment {

    private ImageView mImageView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final RelativeLayout returnView = (RelativeLayout)inflater.inflate(R.layout.fragment_loading_file_image, container, false);
        final RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) returnView.getLayoutParams();

        // force the layout to generate
        returnView.requestLayout();
        // may need to measure instead
//        returnView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        if (!getResources().getBoolean(R.bool.is_landscape))
            layoutParams.width = returnView.getMeasuredHeight();
//            layoutParams.height = returnView.getMeasuredWidth();
//        else
//            layoutParams.width = returnView.getMeasuredHeight();

        mImageView = (ImageView) returnView.findViewById(R.id.imgFileThumbnail);

        return returnView;
    }

    public void setImageView(final ImageView imageView) {
        mImageView = imageView;
    }
}
