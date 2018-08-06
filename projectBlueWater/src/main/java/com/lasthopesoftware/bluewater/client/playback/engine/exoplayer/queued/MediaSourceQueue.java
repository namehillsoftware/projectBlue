package com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.PlayerMessage;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.CompositeMediaSource;
import com.google.android.exoplayer2.source.DeferredMediaPeriod;
import com.google.android.exoplayer2.source.ForwardingTimeline;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ShuffleOrder;
import com.google.android.exoplayer2.source.ShuffleOrder.DefaultShuffleOrder;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Concatenates multiple {@link MediaSource}s. The list of {@link MediaSource}s can be modified
 * during playback. It is valid for the same {@link MediaSource} instance to be present more than
 * once in the concatenation. Access to this class is thread-safe.
 */
public class MediaSourceQueue extends CompositeMediaSource<MediaSourceQueue.MediaSourceHolder>
	implements PlayerMessage.Target {

	private static final int MSG_ADD = 0;
	private static final int MSG_ADD_MULTIPLE = 1;
	private static final int MSG_REMOVE = 2;
	private static final int MSG_MOVE = 3;
	private static final int MSG_CLEAR = 4;
	private static final int MSG_NOTIFY_LISTENER = 5;
	private static final int MSG_ON_COMPLETION = 6;

	// Accessed on the app thread.
	private final List<MediaSourceHolder> mediaSourcesPublic;

	// Accessed on the playback thread.
	private final List<MediaSourceHolder> mediaSourceHolders;
	private final MediaSourceHolder query;
	private final Map<MediaPeriod, MediaSourceHolder> mediaSourceByMediaPeriod;
	private final List<EventDispatcher> pendingOnCompletionActions;
	private final boolean isAtomic;
	private final Timeline.Window window;

	private ExoPlayer player;
	private boolean listenerNotificationScheduled;
	private ShuffleOrder shuffleOrder;
	private int windowCount;
	private int periodCount;

	/** Creates a new concatenating media source. */
	public MediaSourceQueue() {
		this(/* isAtomic= */ false, new DefaultShuffleOrder(0));
	}

	/**
	 * Creates a new concatenating media source.
	 *
	 * @param isAtomic Whether the concatenating media source will be treated as atomic, i.e., treated
	 *     as a single item for repeating and shuffling.
	 */
	public MediaSourceQueue(boolean isAtomic) {
		this(isAtomic, new DefaultShuffleOrder(0));
	}

	/**
	 * Creates a new concatenating media source with a custom shuffle order.
	 *
	 * @param isAtomic Whether the concatenating media source will be treated as atomic, i.e., treated
	 *     as a single item for repeating and shuffling.
	 * @param shuffleOrder The {@link ShuffleOrder} to use when shuffling the child media sources.
	 */
	public MediaSourceQueue(boolean isAtomic, ShuffleOrder shuffleOrder) {
		this(isAtomic, shuffleOrder, new MediaSource[0]);
	}

	/**
	 * @param mediaSources The {@link MediaSource}s to concatenate. It is valid for the same
	 *     {@link MediaSource} instance to be present more than once in the array.
	 */
	public MediaSourceQueue(MediaSource... mediaSources) {
		this(/* isAtomic= */ false, mediaSources);
	}

	/**
	 * @param isAtomic Whether the concatenating media source will be treated as atomic, i.e., treated
	 *     as a single item for repeating and shuffling.
	 * @param mediaSources The {@link MediaSource}s to concatenate. It is valid for the same {@link
	 *     MediaSource} instance to be present more than once in the array.
	 */
	public MediaSourceQueue(boolean isAtomic, MediaSource... mediaSources) {
		this(isAtomic, new DefaultShuffleOrder(0), mediaSources);
	}

	/**
	 * @param isAtomic Whether the concatenating media source will be treated as atomic, i.e., treated
	 *     as a single item for repeating and shuffling.
	 * @param shuffleOrder The {@link ShuffleOrder} to use when shuffling the child media sources.
	 * @param mediaSources The {@link MediaSource}s to concatenate. It is valid for the same {@link
	 *     MediaSource} instance to be present more than once in the array.
	 */
	public MediaSourceQueue(
		boolean isAtomic, ShuffleOrder shuffleOrder, MediaSource... mediaSources) {
		for (MediaSource mediaSource : mediaSources) {
			Assertions.checkNotNull(mediaSource);
		}
		this.shuffleOrder = shuffleOrder.getLength() > 0 ? shuffleOrder.cloneAndClear() : shuffleOrder;
		this.mediaSourceByMediaPeriod = new IdentityHashMap<>();
		this.mediaSourcesPublic = new ArrayList<>();
		this.mediaSourceHolders = new ArrayList<>();
		this.pendingOnCompletionActions = new ArrayList<>();
		this.query = new MediaSourceHolder(/* mediaSource= */ null);
		this.isAtomic = isAtomic;
		window = new Timeline.Window();
		addMediaSources(Arrays.asList(mediaSources));
	}

	public final synchronized Promise<Void> enqueueMediaSource(
		MediaSource mediaSource) {
		return new Promise<>(m -> addMediaSource(mediaSourcesPublic.size(), mediaSource, () -> m.sendResolution(null)));
	}

	private synchronized void addMediaSource(
		int index, MediaSource mediaSource, @Nullable Runnable actionOnCompletion) {
		Assertions.checkNotNull(mediaSource);
		MediaSourceHolder mediaSourceHolder = new MediaSourceHolder(mediaSource);
		mediaSourcesPublic.add(index, mediaSourceHolder);
		if (player != null) {
			player
				.createMessage(this)
				.setType(MSG_ADD)
				.setPayload(new MessageData<>(index, mediaSourceHolder, actionOnCompletion))
				.send();
		} else if (actionOnCompletion != null) {
			actionOnCompletion.run();
		}
	}

	private synchronized void addMediaSources(Collection<MediaSource> mediaSources) {
		addMediaSources(mediaSourcesPublic.size(), mediaSources, null);
	}

	private final synchronized void addMediaSources(
		int index, Collection<MediaSource> mediaSources, @Nullable Runnable actionOnCompletion) {
		for (MediaSource mediaSource : mediaSources) {
			Assertions.checkNotNull(mediaSource);
		}
		List<MediaSourceHolder> mediaSourceHolders = new ArrayList<>(mediaSources.size());
		for (MediaSource mediaSource : mediaSources) {
			mediaSourceHolders.add(new MediaSourceHolder(mediaSource));
		}
		mediaSourcesPublic.addAll(index, mediaSourceHolders);
		if (player != null && !mediaSources.isEmpty()) {
			player
				.createMessage(this)
				.setType(MSG_ADD_MULTIPLE)
				.setPayload(new MessageData<>(index, mediaSourceHolders, actionOnCompletion))
				.send();
		} else if (actionOnCompletion != null) {
			actionOnCompletion.run();
		}
	}

	public final synchronized void removeMediaSource(int index) {
		removeMediaSource(index, null);
	}

	public final synchronized void removeMediaSource(
		int index, @Nullable Runnable actionOnCompletion) {
		mediaSourcesPublic.remove(index);
		if (player != null) {
			player
				.createMessage(this)
				.setType(MSG_REMOVE)
				.setPayload(new MessageData<>(index, null, actionOnCompletion))
				.send();
		} else if (actionOnCompletion != null) {
			actionOnCompletion.run();
		}
	}

	public final synchronized void clear() {
		clear(/* actionOnCompletion= */ null);
	}

	public final synchronized void clear(@Nullable Runnable actionOnCompletion) {
		mediaSourcesPublic.clear();
		if (player != null) {
			player
				.createMessage(this)
				.setType(MSG_CLEAR)
				.setPayload(actionOnCompletion != null ? new EventDispatcher(actionOnCompletion) : null)
				.send();
		} else if (actionOnCompletion != null) {
			actionOnCompletion.run();
		}
	}

	public final synchronized int getSize() {
		return mediaSourcesPublic.size();
	}

	public final synchronized MediaSource getMediaSource(int index) {
		return mediaSourcesPublic.get(index).mediaSource;
	}

	@Override
	public final synchronized void prepareSourceInternal(ExoPlayer player, boolean isTopLevelSource) {
		super.prepareSourceInternal(player, isTopLevelSource);
		this.player = player;
		if (mediaSourcesPublic.isEmpty()) {
			notifyListener();
		} else {
			shuffleOrder = shuffleOrder.cloneAndInsert(0, mediaSourcesPublic.size());
			addMediaSourcesInternal(0, mediaSourcesPublic);
			scheduleListenerNotification(/* actionOnCompletion= */ null);
		}
	}

	@Override
	public final MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator) {
		int mediaSourceHolderIndex = findMediaSourceHolderByPeriodIndex(id.periodIndex);
		MediaSourceHolder holder = mediaSourceHolders.get(mediaSourceHolderIndex);
		MediaPeriodId idInSource =
			id.copyWithPeriodIndex(id.periodIndex - holder.firstPeriodIndexInChild);
		DeferredMediaPeriod mediaPeriod =
			new DeferredMediaPeriod(holder.mediaSource, idInSource, allocator);
		mediaSourceByMediaPeriod.put(mediaPeriod, holder);
		holder.activeMediaPeriods.add(mediaPeriod);
		if (holder.isPrepared) {
			mediaPeriod.createPeriod();
		}
		return mediaPeriod;
	}

	@Override
	public final void releasePeriod(MediaPeriod mediaPeriod) {
		MediaSourceHolder holder = mediaSourceByMediaPeriod.remove(mediaPeriod);
		((DeferredMediaPeriod) mediaPeriod).releasePeriod();
		holder.activeMediaPeriods.remove(mediaPeriod);
		if (holder.activeMediaPeriods.isEmpty() && holder.isRemoved) {
			releaseChildSource(holder);
		}
	}

	@Override
	public final void releaseSourceInternal() {
		super.releaseSourceInternal();
		mediaSourceHolders.clear();
		player = null;
		shuffleOrder = shuffleOrder.cloneAndClear();
		windowCount = 0;
		periodCount = 0;
	}

	@Override
	protected final void onChildSourceInfoRefreshed(
		MediaSourceHolder mediaSourceHolder,
		MediaSource mediaSource,
		Timeline timeline,
		@Nullable Object manifest) {
		updateMediaSourceInternal(mediaSourceHolder, timeline);
	}

	@Override
	protected @Nullable MediaPeriodId getMediaPeriodIdForChildMediaPeriodId(
		MediaSourceHolder mediaSourceHolder, MediaPeriodId mediaPeriodId) {
		for (int i = 0; i < mediaSourceHolder.activeMediaPeriods.size(); i++) {
			// Ensure the reported media period id has the same window sequence number as the one created
			// by this media source. Otherwise it does not belong to this child source.
			if (mediaSourceHolder.activeMediaPeriods.get(i).id.windowSequenceNumber
				== mediaPeriodId.windowSequenceNumber) {
				return mediaPeriodId.copyWithPeriodIndex(
					mediaPeriodId.periodIndex + mediaSourceHolder.firstPeriodIndexInChild);
			}
		}
		return null;
	}

	@Override
	protected int getWindowIndexForChildWindowIndex(
		MediaSourceHolder mediaSourceHolder, int windowIndex) {
		return windowIndex + mediaSourceHolder.firstWindowIndexInChild;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final void handleMessage(int messageType, Object message) throws ExoPlaybackException {
		switch (messageType) {
			case MSG_ADD:
				MessageData<MediaSourceHolder> addMessage = (MessageData<MediaSourceHolder>) message;
				shuffleOrder = shuffleOrder.cloneAndInsert(addMessage.index, 1);
				addMediaSourceInternal(addMessage.index, addMessage.customData);
				scheduleListenerNotification(addMessage.actionOnCompletion);
				break;
			case MSG_ADD_MULTIPLE:
				MessageData<Collection<MediaSourceHolder>> addMultipleMessage =
					(MessageData<Collection<MediaSourceHolder>>) message;
				shuffleOrder =
					shuffleOrder.cloneAndInsert(
						addMultipleMessage.index, addMultipleMessage.customData.size());
				addMediaSourcesInternal(addMultipleMessage.index, addMultipleMessage.customData);
				scheduleListenerNotification(addMultipleMessage.actionOnCompletion);
				break;
			case MSG_REMOVE:
				MessageData<Void> removeMessage = (MessageData<Void>) message;
				shuffleOrder = shuffleOrder.cloneAndRemove(removeMessage.index);
				removeMediaSourceInternal(removeMessage.index);
				scheduleListenerNotification(removeMessage.actionOnCompletion);
				break;
			case MSG_MOVE:
				MessageData<Integer> moveMessage = (MessageData<Integer>) message;
				shuffleOrder = shuffleOrder.cloneAndRemove(moveMessage.index);
				shuffleOrder = shuffleOrder.cloneAndInsert(moveMessage.customData, 1);
				moveMediaSourceInternal(moveMessage.index, moveMessage.customData);
				scheduleListenerNotification(moveMessage.actionOnCompletion);
				break;
			case MSG_CLEAR:
				clearInternal();
				scheduleListenerNotification((EventDispatcher) message);
				break;
			case MSG_NOTIFY_LISTENER:
				notifyListener();
				break;
			case MSG_ON_COMPLETION:
				List<EventDispatcher> actionsOnCompletion = ((List<EventDispatcher>) message);
				for (int i = 0; i < actionsOnCompletion.size(); i++) {
					actionsOnCompletion.get(i).dispatchEvent();
				}
				break;
			default:
				throw new IllegalStateException();
		}
	}

	private void scheduleListenerNotification(@Nullable EventDispatcher actionOnCompletion) {
		if (!listenerNotificationScheduled) {
			player.createMessage(this).setType(MSG_NOTIFY_LISTENER).send();
			listenerNotificationScheduled = true;
		}
		if (actionOnCompletion != null) {
			pendingOnCompletionActions.add(actionOnCompletion);
		}
	}

	private void notifyListener() {
		listenerNotificationScheduled = false;
		List<EventDispatcher> actionsOnCompletion =
			pendingOnCompletionActions.isEmpty()
				? Collections.<EventDispatcher>emptyList()
				: new ArrayList<>(pendingOnCompletionActions);
		pendingOnCompletionActions.clear();
		refreshSourceInfo(
			new ConcatenatedTimeline(
				mediaSourceHolders, windowCount, periodCount, shuffleOrder, isAtomic),
			/* manifest= */ null);
		if (!actionsOnCompletion.isEmpty()) {
			player.createMessage(this).setType(MSG_ON_COMPLETION).setPayload(actionsOnCompletion).send();
		}
	}

	private void addMediaSourceInternal(int newIndex, MediaSourceHolder newMediaSourceHolder) {
		if (newIndex > 0) {
			MediaSourceHolder previousHolder = mediaSourceHolders.get(newIndex - 1);
			newMediaSourceHolder.reset(
				newIndex,
				previousHolder.firstWindowIndexInChild + previousHolder.timeline.getWindowCount(),
				previousHolder.firstPeriodIndexInChild + previousHolder.timeline.getPeriodCount());
		} else {
			newMediaSourceHolder.reset(
				newIndex, /* firstWindowIndexInChild= */ 0, /* firstPeriodIndexInChild= */ 0);
		}
		correctOffsets(
			newIndex,
			/* childIndexUpdate= */ 1,
			newMediaSourceHolder.timeline.getWindowCount(),
			newMediaSourceHolder.timeline.getPeriodCount());
		mediaSourceHolders.add(newIndex, newMediaSourceHolder);
		prepareChildSource(newMediaSourceHolder, newMediaSourceHolder.mediaSource);
	}

	private void addMediaSourcesInternal(
		int index, Collection<MediaSourceHolder> mediaSourceHolders) {
		for (MediaSourceHolder mediaSourceHolder : mediaSourceHolders) {
			addMediaSourceInternal(index++, mediaSourceHolder);
		}
	}

	private void updateMediaSourceInternal(MediaSourceHolder mediaSourceHolder, Timeline timeline) {
		if (mediaSourceHolder == null) {
			throw new IllegalArgumentException();
		}
		DeferredTimeline deferredTimeline = mediaSourceHolder.timeline;
		if (deferredTimeline.getTimeline() == timeline) {
			return;
		}
		int windowOffsetUpdate = timeline.getWindowCount() - deferredTimeline.getWindowCount();
		int periodOffsetUpdate = timeline.getPeriodCount() - deferredTimeline.getPeriodCount();
		if (windowOffsetUpdate != 0 || periodOffsetUpdate != 0) {
			correctOffsets(
				mediaSourceHolder.childIndex + 1,
				/* childIndexUpdate= */ 0,
				windowOffsetUpdate,
				periodOffsetUpdate);
		}
		mediaSourceHolder.timeline = deferredTimeline.cloneWithNewTimeline(timeline);
		if (!mediaSourceHolder.isPrepared && !timeline.isEmpty()) {
			timeline.getWindow(/* windowIndex= */ 0, window);
			long defaultPeriodPositionUs =
				window.getPositionInFirstPeriodUs() + window.getDefaultPositionUs();
			for (int i = 0; i < mediaSourceHolder.activeMediaPeriods.size(); i++) {
				DeferredMediaPeriod deferredMediaPeriod = mediaSourceHolder.activeMediaPeriods.get(i);
				deferredMediaPeriod.setDefaultPreparePositionUs(defaultPeriodPositionUs);
				deferredMediaPeriod.createPeriod();
			}
			mediaSourceHolder.isPrepared = true;
		}
		scheduleListenerNotification(/* actionOnCompletion= */ null);
	}

	private void clearInternal() {
		for (int index = mediaSourceHolders.size() - 1; index >= 0; index--) {
			removeMediaSourceInternal(index);
		}
	}

	private void removeMediaSourceInternal(int index) {
		MediaSourceHolder holder = mediaSourceHolders.remove(index);
		Timeline oldTimeline = holder.timeline;
		correctOffsets(
			index,
			/* childIndexUpdate= */ -1,
			-oldTimeline.getWindowCount(),
			-oldTimeline.getPeriodCount());
		holder.isRemoved = true;
		if (holder.activeMediaPeriods.isEmpty()) {
			releaseChildSource(holder);
		}
	}

	private void moveMediaSourceInternal(int currentIndex, int newIndex) {
		int startIndex = Math.min(currentIndex, newIndex);
		int endIndex = Math.max(currentIndex, newIndex);
		int windowOffset = mediaSourceHolders.get(startIndex).firstWindowIndexInChild;
		int periodOffset = mediaSourceHolders.get(startIndex).firstPeriodIndexInChild;
		mediaSourceHolders.add(newIndex, mediaSourceHolders.remove(currentIndex));
		for (int i = startIndex; i <= endIndex; i++) {
			MediaSourceHolder holder = mediaSourceHolders.get(i);
			holder.firstWindowIndexInChild = windowOffset;
			holder.firstPeriodIndexInChild = periodOffset;
			windowOffset += holder.timeline.getWindowCount();
			periodOffset += holder.timeline.getPeriodCount();
		}
	}

	private void correctOffsets(
		int startIndex, int childIndexUpdate, int windowOffsetUpdate, int periodOffsetUpdate) {
		windowCount += windowOffsetUpdate;
		periodCount += periodOffsetUpdate;
		for (int i = startIndex; i < mediaSourceHolders.size(); i++) {
			mediaSourceHolders.get(i).childIndex += childIndexUpdate;
			mediaSourceHolders.get(i).firstWindowIndexInChild += windowOffsetUpdate;
			mediaSourceHolders.get(i).firstPeriodIndexInChild += periodOffsetUpdate;
		}
	}

	private int findMediaSourceHolderByPeriodIndex(int periodIndex) {
		query.firstPeriodIndexInChild = periodIndex;
		int index = Collections.binarySearch(mediaSourceHolders, query);
		if (index < 0) {
			return -index - 2;
		}
		while (index < mediaSourceHolders.size() - 1
			&& mediaSourceHolders.get(index + 1).firstPeriodIndexInChild == periodIndex) {
			index++;
		}
		return index;
	}

	/** Data class to hold playlist media sources together with meta data needed to process them. */
	/* package */ static final class MediaSourceHolder implements Comparable<MediaSourceHolder> {

		public final MediaSource mediaSource;
		public final Object uid;

		public DeferredTimeline timeline;
		public int childIndex;
		public int firstWindowIndexInChild;
		public int firstPeriodIndexInChild;
		public boolean isPrepared;
		public boolean isRemoved;
		public List<DeferredMediaPeriod> activeMediaPeriods;

		public MediaSourceHolder(MediaSource mediaSource) {
			this.mediaSource = mediaSource;
			this.timeline = new DeferredTimeline();
			this.activeMediaPeriods = new ArrayList<>();
			this.uid = new Object();
		}

		public void reset(int childIndex, int firstWindowIndexInChild, int firstPeriodIndexInChild) {
			this.childIndex = childIndex;
			this.firstWindowIndexInChild = firstWindowIndexInChild;
			this.firstPeriodIndexInChild = firstPeriodIndexInChild;
			this.isPrepared = false;
			this.isRemoved = false;
			this.activeMediaPeriods.clear();
		}

		@Override
		public int compareTo(@NonNull MediaSourceHolder other) {
			return this.firstPeriodIndexInChild - other.firstPeriodIndexInChild;
		}
	}

	/** Can be used to dispatch a runnable on the thread the object was created on. */
	private static final class EventDispatcher {

		public final Handler eventHandler;
		public final Runnable runnable;

		public EventDispatcher(Runnable runnable) {
			this.runnable = runnable;
			this.eventHandler =
				new Handler(Looper.myLooper() != null ? Looper.myLooper() : Looper.getMainLooper());
		}

		public void dispatchEvent() {
			eventHandler.post(runnable);
		}
	}

	/** Message used to post actions from app thread to playback thread. */
	private static final class MessageData<T> {

		public final int index;
		public final T customData;
		public final @Nullable EventDispatcher actionOnCompletion;

		public MessageData(int index, T customData, @Nullable Runnable actionOnCompletion) {
			this.index = index;
			this.actionOnCompletion =
				actionOnCompletion != null ? new EventDispatcher(actionOnCompletion) : null;
			this.customData = customData;
		}
	}

	private static final class ConcatenatedTimeline extends Timeline {

		private final int childCount;
		private final ShuffleOrder shuffleOrder;
		private final boolean isAtomic;
		private final int windowCount;
		private final int periodCount;
		private final int[] firstPeriodInChildIndices;
		private final int[] firstWindowInChildIndices;
		private final Timeline[] timelines;
		private final Object[] uids;
		private final HashMap<Object, Integer> childIndexByUid;

		public ConcatenatedTimeline(
			Collection<MediaSourceHolder> mediaSourceHolders,
			int windowCount,
			int periodCount,
			ShuffleOrder shuffleOrder,
			boolean isAtomic) {
			this.isAtomic = isAtomic;
			this.shuffleOrder = shuffleOrder;
			this.windowCount = windowCount;
			this.periodCount = periodCount;
			childCount = mediaSourceHolders.size();
			firstPeriodInChildIndices = new int[childCount];
			firstWindowInChildIndices = new int[childCount];
			timelines = new Timeline[childCount];
			uids = new Object[childCount];
			childIndexByUid = new HashMap<>();
			int index = 0;
			for (MediaSourceHolder mediaSourceHolder : mediaSourceHolders) {
				timelines[index] = mediaSourceHolder.timeline;
				firstPeriodInChildIndices[index] = mediaSourceHolder.firstPeriodIndexInChild;
				firstWindowInChildIndices[index] = mediaSourceHolder.firstWindowIndexInChild;
				uids[index] = mediaSourceHolder.uid;
				childIndexByUid.put(uids[index], index++);
			}
		}

		protected int getChildIndexByPeriodIndex(int periodIndex) {
			return Util.binarySearchFloor(firstPeriodInChildIndices, periodIndex + 1, false, false);
		}

		protected int getChildIndexByWindowIndex(int windowIndex) {
			return Util.binarySearchFloor(firstWindowInChildIndices, windowIndex + 1, false, false);
		}

		protected int getChildIndexByChildUid(Object childUid) {
			Integer index = childIndexByUid.get(childUid);
			return index == null ? C.INDEX_UNSET : index;
		}

		protected Timeline getTimelineByChildIndex(int childIndex) {
			return timelines[childIndex];
		}

		protected int getFirstPeriodIndexByChildIndex(int childIndex) {
			return firstPeriodInChildIndices[childIndex];
		}

		protected int getFirstWindowIndexByChildIndex(int childIndex) {
			return firstWindowInChildIndices[childIndex];
		}

		protected Object getChildUidByChildIndex(int childIndex) {
			return uids[childIndex];
		}

		public int getWindowCount() {
			return windowCount;
		}

		public int getPeriodCount() {
			return periodCount;
		}

		public int getNextWindowIndex(int windowIndex, @Player.RepeatMode int repeatMode,
									  boolean shuffleModeEnabled) {
			if (isAtomic) {
				// Adapt repeat and shuffle mode to atomic concatenation.
				repeatMode = repeatMode == Player.REPEAT_MODE_ONE ? Player.REPEAT_MODE_ALL : repeatMode;
				shuffleModeEnabled = false;
			}
			// Find next window within current child.
			int childIndex = getChildIndexByWindowIndex(windowIndex);
			int firstWindowIndexInChild = getFirstWindowIndexByChildIndex(childIndex);
			int nextWindowIndexInChild = getTimelineByChildIndex(childIndex).getNextWindowIndex(
				windowIndex - firstWindowIndexInChild,
				repeatMode == Player.REPEAT_MODE_ALL ? Player.REPEAT_MODE_OFF : repeatMode,
				shuffleModeEnabled);
			if (nextWindowIndexInChild != C.INDEX_UNSET) {
				return firstWindowIndexInChild + nextWindowIndexInChild;
			}
			// If not found, find first window of next non-empty child.
			int nextChildIndex = getNextChildIndex(childIndex, shuffleModeEnabled);
			while (nextChildIndex != C.INDEX_UNSET && getTimelineByChildIndex(nextChildIndex).isEmpty()) {
				nextChildIndex = getNextChildIndex(nextChildIndex, shuffleModeEnabled);
			}
			if (nextChildIndex != C.INDEX_UNSET) {
				return getFirstWindowIndexByChildIndex(nextChildIndex)
					+ getTimelineByChildIndex(nextChildIndex).getFirstWindowIndex(shuffleModeEnabled);
			}
			// If not found, this is the last window.
			if (repeatMode == Player.REPEAT_MODE_ALL) {
				return getFirstWindowIndex(shuffleModeEnabled);
			}
			return C.INDEX_UNSET;
		}

		public int getPreviousWindowIndex(int windowIndex, @Player.RepeatMode int repeatMode,
										  boolean shuffleModeEnabled) {
			if (isAtomic) {
				// Adapt repeat and shuffle mode to atomic concatenation.
				repeatMode = repeatMode == Player.REPEAT_MODE_ONE ? Player.REPEAT_MODE_ALL : repeatMode;
				shuffleModeEnabled = false;
			}
			// Find previous window within current child.
			int childIndex = getChildIndexByWindowIndex(windowIndex);
			int firstWindowIndexInChild = getFirstWindowIndexByChildIndex(childIndex);
			int previousWindowIndexInChild = getTimelineByChildIndex(childIndex).getPreviousWindowIndex(
				windowIndex - firstWindowIndexInChild,
				repeatMode == Player.REPEAT_MODE_ALL ? Player.REPEAT_MODE_OFF : repeatMode,
				shuffleModeEnabled);
			if (previousWindowIndexInChild != C.INDEX_UNSET) {
				return firstWindowIndexInChild + previousWindowIndexInChild;
			}
			// If not found, find last window of previous non-empty child.
			int previousChildIndex = getPreviousChildIndex(childIndex, shuffleModeEnabled);
			while (previousChildIndex != C.INDEX_UNSET
				&& getTimelineByChildIndex(previousChildIndex).isEmpty()) {
				previousChildIndex = getPreviousChildIndex(previousChildIndex, shuffleModeEnabled);
			}
			if (previousChildIndex != C.INDEX_UNSET) {
				return getFirstWindowIndexByChildIndex(previousChildIndex)
					+ getTimelineByChildIndex(previousChildIndex).getLastWindowIndex(shuffleModeEnabled);
			}
			// If not found, this is the first window.
			if (repeatMode == Player.REPEAT_MODE_ALL) {
				return getLastWindowIndex(shuffleModeEnabled);
			}
			return C.INDEX_UNSET;
		}

		public int getLastWindowIndex(boolean shuffleModeEnabled) {
			if (childCount == 0) {
				return C.INDEX_UNSET;
			}
			if (isAtomic) {
				shuffleModeEnabled = false;
			}
			// Find last non-empty child.
			int lastChildIndex = shuffleModeEnabled ? shuffleOrder.getLastIndex() : childCount - 1;
			while (getTimelineByChildIndex(lastChildIndex).isEmpty()) {
				lastChildIndex = getPreviousChildIndex(lastChildIndex, shuffleModeEnabled);
				if (lastChildIndex == C.INDEX_UNSET) {
					// All children are empty.
					return C.INDEX_UNSET;
				}
			}
			return getFirstWindowIndexByChildIndex(lastChildIndex)
				+ getTimelineByChildIndex(lastChildIndex).getLastWindowIndex(shuffleModeEnabled);
		}

		public int getFirstWindowIndex(boolean shuffleModeEnabled) {
			if (childCount == 0) {
				return C.INDEX_UNSET;
			}
			if (isAtomic) {
				shuffleModeEnabled = false;
			}
			// Find first non-empty child.
			int firstChildIndex = shuffleModeEnabled ? shuffleOrder.getFirstIndex() : 0;
			while (getTimelineByChildIndex(firstChildIndex).isEmpty()) {
				firstChildIndex = getNextChildIndex(firstChildIndex, shuffleModeEnabled);
				if (firstChildIndex == C.INDEX_UNSET) {
					// All children are empty.
					return C.INDEX_UNSET;
				}
			}
			return getFirstWindowIndexByChildIndex(firstChildIndex)
				+ getTimelineByChildIndex(firstChildIndex).getFirstWindowIndex(shuffleModeEnabled);
		}

		public final Timeline.Window getWindow(
			int windowIndex, Timeline.Window window, boolean setTag, long defaultPositionProjectionUs) {
			int childIndex = getChildIndexByWindowIndex(windowIndex);
			int firstWindowIndexInChild = getFirstWindowIndexByChildIndex(childIndex);
			int firstPeriodIndexInChild = getFirstPeriodIndexByChildIndex(childIndex);
			getTimelineByChildIndex(childIndex)
				.getWindow(
					windowIndex - firstWindowIndexInChild, window, setTag, defaultPositionProjectionUs);
			window.firstPeriodIndex += firstPeriodIndexInChild;
			window.lastPeriodIndex += firstPeriodIndexInChild;
			return window;
		}

		public final Timeline.Period getPeriod(int periodIndex, Timeline.Period period, boolean setIds) {
			int childIndex = getChildIndexByPeriodIndex(periodIndex);
			int firstWindowIndexInChild = getFirstWindowIndexByChildIndex(childIndex);
			int firstPeriodIndexInChild = getFirstPeriodIndexByChildIndex(childIndex);
			getTimelineByChildIndex(childIndex).getPeriod(periodIndex - firstPeriodIndexInChild, period,
				setIds);
			period.windowIndex += firstWindowIndexInChild;
			if (setIds) {
				period.uid = Pair.create(getChildUidByChildIndex(childIndex), period.uid);
			}
			return period;
		}

		public final int getIndexOfPeriod(Object uid) {
			if (!(uid instanceof Pair)) {
				return C.INDEX_UNSET;
			}
			Pair<?, ?> childUidAndPeriodUid = (Pair<?, ?>) uid;
			Object childUid = childUidAndPeriodUid.first;
			Object periodUid = childUidAndPeriodUid.second;
			int childIndex = getChildIndexByChildUid(childUid);
			if (childIndex == C.INDEX_UNSET) {
				return C.INDEX_UNSET;
			}
			int periodIndexInChild = getTimelineByChildIndex(childIndex).getIndexOfPeriod(periodUid);
			return periodIndexInChild == C.INDEX_UNSET ? C.INDEX_UNSET
				: getFirstPeriodIndexByChildIndex(childIndex) + periodIndexInChild;
		}

		private int getNextChildIndex(int childIndex, boolean shuffleModeEnabled) {
			return shuffleModeEnabled ? shuffleOrder.getNextIndex(childIndex)
				: childIndex < childCount - 1 ? childIndex + 1 : C.INDEX_UNSET;
		}

		private int getPreviousChildIndex(int childIndex, boolean shuffleModeEnabled) {
			return shuffleModeEnabled ? shuffleOrder.getPreviousIndex(childIndex)
				: childIndex > 0 ? childIndex - 1 : C.INDEX_UNSET;
		}
	}

	/**
	 * Timeline used as placeholder for an unprepared media source. After preparation, a copy of the
	 * DeferredTimeline is used to keep the originally assigned first period ID.
	 */
	private static final class DeferredTimeline extends ForwardingTimeline {

		private static final Object DUMMY_ID = new Object();
		private static final Period period = new Period();
		private static final DummyTimeline dummyTimeline = new DummyTimeline();

		private final Object replacedId;

		public DeferredTimeline() {
			this(dummyTimeline, /* replacedId= */ null);
		}

		private DeferredTimeline(Timeline timeline, Object replacedId) {
			super(timeline);
			this.replacedId = replacedId;
		}

		public DeferredTimeline cloneWithNewTimeline(Timeline timeline) {
			return new DeferredTimeline(
				timeline,
				replacedId == null && timeline.getPeriodCount() > 0
					? timeline.getPeriod(0, period, true).uid
					: replacedId);
		}

		public Timeline getTimeline() {
			return timeline;
		}

		@Override
		public Period getPeriod(int periodIndex, Period period, boolean setIds) {
			timeline.getPeriod(periodIndex, period, setIds);
			if (Util.areEqual(period.uid, replacedId)) {
				period.uid = DUMMY_ID;
			}
			return period;
		}

		@Override
		public int getIndexOfPeriod(Object uid) {
			return timeline.getIndexOfPeriod(DUMMY_ID.equals(uid) ? replacedId : uid);
		}
	}

	/** Dummy placeholder timeline with one dynamic window with a period of indeterminate duration. */
	private static final class DummyTimeline extends Timeline {

		@Override
		public int getWindowCount() {
			return 1;
		}

		@Override
		public Window getWindow(
			int windowIndex, Window window, boolean setTag, long defaultPositionProjectionUs) {
			return window.set(
				/* tag= */ null,
				/* presentationStartTimeMs= */ C.TIME_UNSET,
				/* windowStartTimeMs= */ C.TIME_UNSET,
				/* isSeekable= */ false,
				// Dynamic window to indicate pending timeline updates.
				/* isDynamic= */ true,
				// Position can't be projected yet as the default position is still unknown.
				/* defaultPositionUs= */ defaultPositionProjectionUs > 0 ? C.TIME_UNSET : 0,
				/* durationUs= */ C.TIME_UNSET,
				/* firstPeriodIndex= */ 0,
				/* lastPeriodIndex= */ 0,
				/* positionInFirstPeriodUs= */ 0);
		}

		@Override
		public int getPeriodCount() {
			return 1;
		}

		@Override
		public Period getPeriod(int periodIndex, Period period, boolean setIds) {
			return period.set(
				/* id= */ null,
				/* uid= */ null,
				/* windowIndex= */ 0,
				/* durationUs = */ C.TIME_UNSET,
				/* positionInWindowUs= */ 0);
		}

		@Override
		public int getIndexOfPeriod(Object uid) {
			return uid == null ? 0 : C.INDEX_UNSET;
		}
	}
}

