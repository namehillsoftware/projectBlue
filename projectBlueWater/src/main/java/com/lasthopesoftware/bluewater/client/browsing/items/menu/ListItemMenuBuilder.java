package com.lasthopesoftware.bluewater.client.browsing.items.menu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.browsing.items.Item;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.IFileListParameterProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.PlayClickHandler;
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ShuffleClickHandler;
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.SyncFilesIsVisibleHandler;
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ViewFilesClickHandler;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess;
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder;

public final class ListItemMenuBuilder extends AbstractListItemMenuBuilder<Item> {

	private static class ViewHolder {
		private final LazyViewFinder<TextView> textViewFinder;
		private final LazyViewFinder<ImageButton> shuffleButtonFinder;
		private final LazyViewFinder<ImageButton> playButtonFinder;
		private final LazyViewFinder<ImageButton> viewButtonFinder;
		private final LazyViewFinder<ImageButton> syncButtonFinder;

		ViewHolder(
			LazyViewFinder<TextView> textViewFinder,
			LazyViewFinder<ImageButton> shuffleButtonFinder,
			LazyViewFinder<ImageButton> playButtonFinder,
			LazyViewFinder<ImageButton> viewButtonFinder,
			LazyViewFinder<ImageButton> syncButtonFinder) {
			this.textViewFinder = textViewFinder;
			this.shuffleButtonFinder = shuffleButtonFinder;
			this.playButtonFinder = playButtonFinder;
			this.viewButtonFinder = viewButtonFinder;
			this.syncButtonFinder = syncButtonFinder;
		}

		View.OnLayoutChangeListener onSyncButtonLayoutChangeListener;

		TextView getTextView() {
			return textViewFinder.findView();
		}

		ImageButton getShuffleButton() {
			return shuffleButtonFinder.findView();
		}

		ImageButton getPlayButton() {
			return playButtonFinder.findView();
		}

		ImageButton getViewButton() {
			return viewButtonFinder.findView();
		}

		ImageButton getSyncButton() {
			return syncButtonFinder.findView();
		}
	}

	private final StoredItemAccess storedItemAccess;
	private final Library library;
	private final IFileListParameterProvider fileListParameterProvider;

	public ListItemMenuBuilder(StoredItemAccess storedItemAccess, Library library, IFileListParameterProvider fileListParameterProvider) {
		this.storedItemAccess = storedItemAccess;
		this.library = library;
		this.fileListParameterProvider = fileListParameterProvider;
	}

	@Override
	public View getView(int position, Item item, View convertView, ViewGroup parent) {
		NotifyOnFlipViewAnimator parentView = (NotifyOnFlipViewAnimator)convertView;
		if (parentView == null) {
		
			final AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
		            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            parentView = new NotifyOnFlipViewAnimator(parent.getContext());
            parentView.setLayoutParams(lp);

			final LayoutInflater inflater = (LayoutInflater) parentView.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			final LinearLayout listItemLayout = (LinearLayout) inflater.inflate(R.layout.layout_list_item, parentView, false);
			parentView.addView(listItemLayout);

			final LinearLayout fileMenu = (LinearLayout)inflater.inflate(R.layout.layout_browse_item_menu, parentView, false);
			parentView.addView(fileMenu);

			parentView.setTag(
				new ViewHolder(
					new LazyViewFinder<>(listItemLayout, R.id.tvListItem),
					new LazyViewFinder<>(fileMenu, R.id.btnShuffle),
					new LazyViewFinder<>(fileMenu, R.id.btnPlayAll),
					new LazyViewFinder<>(fileMenu, R.id.btnViewFiles),
					new LazyViewFinder<>(fileMenu, R.id.btnSyncItem)));
		}

		parentView.setViewChangedListener(getOnViewChangedListener());

		if (parentView.getDisplayedChild() != 0) parentView.showPrevious();
		
		final ViewHolder viewHolder = (ViewHolder) parentView.getTag();
		viewHolder.getTextView().setText(item.getValue());
		viewHolder.getShuffleButton().setOnClickListener(new ShuffleClickHandler(parentView, fileListParameterProvider, item));
		viewHolder.getPlayButton().setOnClickListener(new PlayClickHandler(parentView, fileListParameterProvider, item));
		viewHolder.getViewButton().setOnClickListener(new ViewFilesClickHandler(parentView, item));

		viewHolder.getSyncButton().setEnabled(false);

		if (viewHolder.onSyncButtonLayoutChangeListener != null)
			viewHolder.getSyncButton().removeOnLayoutChangeListener(viewHolder.onSyncButtonLayoutChangeListener);

		viewHolder.onSyncButtonLayoutChangeListener = new SyncFilesIsVisibleHandler(parentView, viewHolder.getSyncButton(), storedItemAccess, library, item);

		viewHolder.getSyncButton().addOnLayoutChangeListener(viewHolder.onSyncButtonLayoutChangeListener);

		return parentView;
	}
}
