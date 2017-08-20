package com.lasthopesoftware.bluewater.client.library.views.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils;

/**
 * Created by david on 11/23/15.
 */
class SelectViewAdapterBuilder {
	private final LayoutInflater layoutInflater;

	public SelectViewAdapterBuilder(Context context) {
		this.layoutInflater = LayoutInflater.from(context);
	}

	public View getView(View convertView, ViewGroup parent, CharSequence viewText, boolean isSelected) {
		if (convertView == null) {
			convertView = layoutInflater.inflate(R.layout.layout_select_views, parent, false);
		}

		final TextView tvViewName = (TextView) convertView.findViewById(R.id.tvViewName);
		tvViewName.setText(viewText);

		final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(tvViewName.getLayoutParams());
		layoutParams.setMargins(0, 0, isSelected ? ViewUtils.dpToPx(parent.getContext(), 10) : 0, 0);
		tvViewName.setLayoutParams(layoutParams);

		return convertView;
	}
}
