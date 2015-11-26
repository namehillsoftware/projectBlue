package com.lasthopesoftware.bluewater.servers.library.view.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;

/**
 * Created by david on 11/23/15.
 */
public class SelectViewAdapterItem {
	public static View getView(View convertView, ViewGroup parent, CharSequence viewText, boolean isSelected) {
		if (convertView == null) {
			final LayoutInflater inflator = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflator.inflate(R.layout.layout_select_views, parent, false);
		}

		final TextView tvViewName = (TextView) convertView.findViewById(R.id.tvViewName);
		tvViewName.setText(viewText);

		final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(tvViewName.getLayoutParams());
		layoutParams.setMargins(0, 0, isSelected ? ViewUtils.dpToPx(parent.getContext(), 10) : 0, 0);
		tvViewName.setLayoutParams(layoutParams);

		return convertView;
	}
}
