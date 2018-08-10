package com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.PlayerMessage;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.CompositeMediaSource;
import com.google.android.exoplayer2.source.DeferredMediaPeriod;
import com.google.android.exoplayer2.source.ForwardingTimeline;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class MediaSourceQueue extends CompositeMediaSource<MediaSourceQueue.MediaSourceHolder>
	implements PlayerMessage.Target {

	private static final int MSG_ADD = 0;
	private static final int MSG_ADD_MULTIPLE = 1;
	private static final int MSG_DE_QUEUE = 2;
	private static final int MSG_CLEAR = 4;
	private static final int MSG_NOTIFY_LISTENER = 5;
	private static final int MSG_ON_COMPLETION = 6;

	// Accessed on the app thread.
	private final List<MediaSourceHolder> mediaSourcesPublic;

	// Accessed on the playback thread.
	private final Deque<MediaSourceHolder> mediaSourceHolders;
	private final MediaSourceHolder query;
	private final Map<MediaPeriod, MediaSourceHolder> mediaSourceByMediaPeriod;
	private final List<EventDispatcher> pendingOnCompletionActions;
	private final Timeline.Window window;

	private ExoPlayer player;
	private boolean listenerNotificationScheduled;
	private int windowCount;
	private int periodCount;

	public MediaSourceQueue() {
		this(new MediaSource[0]);
	}

	private MediaSourceQueue(MediaSource... mediaSources) {
		for (MediaSource mediaSource : mediaSources) {
			Assertions.checkNotNull(mediaSource);
		}
		this.mediaSourceByMediaPeriod = new IdentityHashMap<>();
		this.mediaSourcesPublic = new ArrayList<>();
		this.mediaSourceHolders = new ArrayDeque<>();
		this.pendingOnCompletionActions = new ArrayList<>();
		this.query = new MediaSourceHolder(/* mediaSource= */ null);
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
		addMediaSources(mediaSourcesPublic.size(), mediaSources);
	}

	private synchronized void addMediaSources(
		int index, Collection<MediaSource> mediaSources) {
		for (MediaSource mediaSource : mediaSources) {
			Assertions.checkNotNull(mediaSource);
		}
		List<MediaSourceHolder> mediaSourceHolders = new ArrayList<>(mediaSources.size());
		for (MediaSource mediaSource : mediaSources) {
			mediaSourceHolders.add(new MediaSourceHolder(mediaSource));
		}
		mediaSourcesPublic.addAll(index, mediaSourceHolders);
		if (player == null || mediaSources.isEmpty()) return;

		player
			.createMessage(this)
			.setType(MSG_ADD_MULTIPLE)
			.setPayload(new MessageData<>(index, mediaSourceHolders, null))
			.send();
	}

	public final synchronized Promise<Void> dequeueMediaSource() {
		mediaSourcesPublic.remove(0);

		if (player == null) return Promise.empty();

		return new Promise<>(m -> player
			.createMessage(this)
			.setType(MSG_DE_QUEUE)
			.setPayload(new MessageData<>(0, null, () -> m.sendResolution(null)))
			.send());
	}

	public final synchronized Promise<Void> clear() {
		return new Promise<>(m -> clear(() -> m.sendResolution(null)));
	}

	private synchronized void clear(@Nullable Runnable actionOnCompletion) {
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

	@Override
	public final synchronized void prepareSourceInternal(ExoPlayer player, boolean isTopLevelSource) {
		super.prepareSourceInternal(player, isTopLevelSource);
		this.player = player;
		if (mediaSourcesPublic.isEmpty()) {
			notifyListener();
		} else {
			addMediaSourcesInternal(mediaSourcesPublic);
			scheduleListenerNotification(/* actionOnCompletion= */ null);
		}
	}

	@Override
	public final MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator) {
		final MediaSourceHolder holder = findMediaSourceHolderByPeriodIndex(id.periodIndex);
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
	public final void handleMessage(int messageType, Object message) {
		switch (messageType) {
			case MSG_ADD:
				final MessageData<MediaSourceHolder> addMessage = (MessageData<MediaSourceHolder>) message;
				addMediaSourceInternal(addMessage.customData);
				scheduleListenerNotification(addMessage.actionOnCompletion);
				break;
			case MSG_ADD_MULTIPLE:
				final MessageData<Collection<MediaSourceHolder>> addMultipleMessage =
					(MessageData<Collection<MediaSourceHolder>>) message;
				addMediaSourcesInternal(addMultipleMessage.customData);
				scheduleListenerNotification(addMultipleMessage.actionOnCompletion);
				break;
			case MSG_DE_QUEUE:
				final MessageData<Void> removeMessage = (MessageData<Void>) message;
				dequeueMediaSourceInternal();
				scheduleListenerNotification(removeMessage.actionOnCompletion);
				break;
			case MSG_CLEAR:
				clearInternal();
				scheduleListenerNotification((EventDispatcher) message);
				break;
			case MSG_NOTIFY_LISTENER:
				notifyListener();
				break;
			case MSG_ON_COMPLETION:
				final List<EventDispatcher> actionsOnCompletion = ((List<EventDispatcher>) message);
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
				? Collections.emptyList()
				: new ArrayList<>(pendingOnCompletionActions);
		pendingOnCompletionActions.clear();
		refreshSourceInfo(
			new QueueTimeline(
				mediaSourceHolders, windowCount, periodCount),
			/* manifest= */ null);
		if (!actionsOnCompletion.isEmpty()) {
			player.createMessage(this).setType(MSG_ON_COMPLETION).setPayload(actionsOnCompletion).send();
		}
	}

	private void addMediaSourceInternal(MediaSourceHolder newMediaSourceHolder) {

		if (!mediaSourceHolders.isEmpty()) {
			final MediaSourceHolder previousHolder = mediaSourceHolders.getLast();
			newMediaSourceHolder.reset(
				previousHolder.firstWindowIndexInChild + previousHolder.timeline.getWindowCount(),
				previousHolder.firstPeriodIndexInChild + previousHolder.timeline.getPeriodCount());
		}

		mediaSourceHolders.offer(newMediaSourceHolder);
		prepareChildSource(newMediaSourceHolder, newMediaSourceHolder.mediaSource);
	}

	private void addMediaSourcesInternal(Collection<MediaSourceHolder> mediaSourceHolders) {
		for (MediaSourceHolder mediaSourceHolder : mediaSourceHolders) {
			addMediaSourceInternal(mediaSourceHolder);
		}
	}

	private void updateMediaSourceInternal(MediaSourceHolder mediaSourceHolder, Timeline timeline) {
		if (mediaSourceHolder == null) {
			throw new IllegalArgumentException();
		}

		final DeferredTimeline deferredTimeline = mediaSourceHolder.timeline;
		if (deferredTimeline.getTimeline() == timeline) return;

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
		while (!mediaSourceHolders.isEmpty()) {
			dequeueMediaSourceInternal();
		}
	}

	private void dequeueMediaSourceInternal() {
		final MediaSourceHolder holder = mediaSourceHolders.poll();
		holder.isRemoved = true;
		if (holder.activeMediaPeriods.isEmpty()) {
			releaseChildSource(holder);
		}
	}

	private MediaSourceHolder findMediaSourceHolderByPeriodIndex(int periodIndex) {
		query.firstPeriodIndexInChild = periodIndex;
		final ArrayList<MediaSourceHolder> holdersList = new ArrayList<>(mediaSourceHolders);
		int index = Collections.binarySearch(holdersList, query);
		if (index < 0) {
			return holdersList.get(-index - 2);
		}
		while (index < holdersList.size() - 1
			&& holdersList.get(index + 1).firstPeriodIndexInChild == periodIndex) {
			index++;
		}
		return holdersList.get(index);
	}

	/** Data class to hold playlist media sources together with meta data needed to process them. */
	static final class MediaSourceHolder implements Comparable<MediaSourceHolder> {

		final MediaSource mediaSource;
		public final Object uid;

		public DeferredTimeline timeline;
		public int firstWindowIndexInChild;
		public int firstPeriodIndexInChild;
		boolean isPrepared;
		boolean isRemoved;
		List<DeferredMediaPeriod> activeMediaPeriods;

		MediaSourceHolder(MediaSource mediaSource) {
			this.mediaSource = mediaSource;
			this.timeline = new DeferredTimeline();
			this.activeMediaPeriods = new ArrayList<>();
			this.uid = new Object();
		}

		void reset(int firstWindowIndexInChild, int firstPeriodIndexInChild) {
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

		final Handler eventHandler;
		public final Runnable runnable;

		EventDispatcher(Runnable runnable) {
			this.runnable = runnable;
			this.eventHandler =
				new Handler(Looper.myLooper() != null ? Looper.myLooper() : Looper.getMainLooper());
		}

		void dispatchEvent() {
			eventHandler.post(runnable);
		}
	}

	/** Message used to post actions from app thread to playback thread. */
	private static final class MessageData<T> {

		final int index;
		final T customData;
		final @Nullable EventDispatcher actionOnCompletion;

		MessageData(int index, T customData, @Nullable Runnable actionOnCompletion) {
			this.index = index;
			this.actionOnCompletion =
				actionOnCompletion != null ? new EventDispatcher(actionOnCompletion) : null;
			this.customData = customData;
		}
	}

	private static final class QueueTimeline extends Timeline {

		private final int childCount;
		private final int windowCount;
		private final int periodCount;
		private final int[] firstPeriodInChildIndices;
		private final int[] firstWindowInChildIndices;
		private final Timeline[] timelines;
		private final Object[] uids;
		private final HashMap<Object, Integer> childIndexByUid;

		QueueTimeline(
			Collection<MediaSourceHolder> mediaSourceHolders,
			int windowCount,
			int periodCount) {
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

		int getChildIndexByPeriodIndex(int periodIndex) {
			return Util.binarySearchFloor(firstPeriodInChildIndices, periodIndex + 1, false, false);
		}

		int getChildIndexByWindowIndex(int windowIndex) {
			return Util.binarySearchFloor(firstWindowInChildIndices, windowIndex + 1, false, false);
		}

		int getChildIndexByChildUid(Object childUid) {
			Integer index = childIndexByUid.get(childUid);
			return index == null ? C.INDEX_UNSET : index;
		}

		Timeline getTimelineByChildIndex(int childIndex) {
			return timelines[childIndex];
		}

		int getFirstPeriodIndexByChildIndex(int childIndex) {
			return firstPeriodInChildIndices[childIndex];
		}

		int getFirstWindowIndexByChildIndex(int childIndex) {
			return firstWindowInChildIndices[childIndex];
		}

		Object getChildUidByChildIndex(int childIndex) {
			return uids[childIndex];
		}

		@Override
		public int getWindowCount() {
			return windowCount;
		}

		@Override
		public int getPeriodCount() {
			return periodCount;
		}

		@Override
		public int getNextWindowIndex(int windowIndex, @Player.RepeatMode int repeatMode,
									  boolean shuffleModeEnabled) {

			// Find next window within current child.
			final int childIndex = getChildIndexByWindowIndex(windowIndex);
			final int firstWindowIndexInChild = getFirstWindowIndexByChildIndex(childIndex);
			final int nextWindowIndexInChild = getTimelineByChildIndex(childIndex).getNextWindowIndex(
				windowIndex - firstWindowIndexInChild,
				Player.REPEAT_MODE_OFF,
				shuffleModeEnabled);
			if (nextWindowIndexInChild != C.INDEX_UNSET) {
				return firstWindowIndexInChild + nextWindowIndexInChild;
			}
			// If not found, find first window of next non-empty child.
			int nextChildIndex = getNextChildIndex(childIndex);
			while (nextChildIndex != C.INDEX_UNSET && getTimelineByChildIndex(nextChildIndex).isEmpty()) {
				nextChildIndex = getNextChildIndex(nextChildIndex);
			}
			if (nextChildIndex != C.INDEX_UNSET) {
				return getFirstWindowIndexByChildIndex(nextChildIndex)
					+ getTimelineByChildIndex(nextChildIndex).getFirstWindowIndex(false);
			}

			return C.INDEX_UNSET;
		}

		@Override
		public int getPreviousWindowIndex(int windowIndex, @Player.RepeatMode int repeatMode,
										  boolean shuffleModeEnabled) {

			// Find previous window within current child.
			int childIndex = getChildIndexByWindowIndex(windowIndex);
			int firstWindowIndexInChild = getFirstWindowIndexByChildIndex(childIndex);
			int previousWindowIndexInChild = getTimelineByChildIndex(childIndex).getPreviousWindowIndex(
				windowIndex - firstWindowIndexInChild,
				repeatMode,
				shuffleModeEnabled);
			if (previousWindowIndexInChild != C.INDEX_UNSET) {
				return firstWindowIndexInChild + previousWindowIndexInChild;
			}

			// If not found, find last window of previous non-empty child.
			int previousChildIndex = getPreviousChildIndex(childIndex);
			while (previousChildIndex != C.INDEX_UNSET
				&& getTimelineByChildIndex(previousChildIndex).isEmpty()) {
				previousChildIndex = getPreviousChildIndex(previousChildIndex);
			}
			if (previousChildIndex != C.INDEX_UNSET) {
				return getFirstWindowIndexByChildIndex(previousChildIndex)
					+ getTimelineByChildIndex(previousChildIndex).getLastWindowIndex(false);
			}

			return C.INDEX_UNSET;
		}

		@Override
		public int getLastWindowIndex(boolean shuffleModeEnabled) {
			if (childCount == 0) {
				return C.INDEX_UNSET;
			}

			// Find last non-empty child.
			int index = childCount - 1;
			while (getTimelineByChildIndex(index).isEmpty()) {
				index = getPreviousChildIndex(index);
				if (index == C.INDEX_UNSET) {
					// All children are empty.
					return C.INDEX_UNSET;
				}
			}
			return getFirstWindowIndexByChildIndex(index)
				+ getTimelineByChildIndex(index).getLastWindowIndex(false);
		}

		@Override
		public int getFirstWindowIndex(boolean shuffleModeEnabled) {
			if (childCount == 0) {
				return C.INDEX_UNSET;
			}

			// Find first non-empty child.
			int index = 0;
			while (getTimelineByChildIndex(index).isEmpty()) {
				index = getNextChildIndex(index);
				if (index == C.INDEX_UNSET) {
					// All children are empty.
					return C.INDEX_UNSET;
				}
			}

			return getFirstWindowIndexByChildIndex(index)
				+ getTimelineByChildIndex(index).getFirstWindowIndex(false);
		}

		@Override
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

		@Override
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

		@Override
		public final int getIndexOfPeriod(Object uid) {
			if (!(uid instanceof Pair)) {
				return C.INDEX_UNSET;
			}
			final Pair<?, ?> childUidAndPeriodUid = (Pair<?, ?>) uid;
			final Object childUid = childUidAndPeriodUid.first;
			final Object periodUid = childUidAndPeriodUid.second;
			final int childIndex = getChildIndexByChildUid(childUid);
			if (childIndex == C.INDEX_UNSET) {
				return C.INDEX_UNSET;
			}
			int periodIndexInChild = getTimelineByChildIndex(childIndex).getIndexOfPeriod(periodUid);
			return periodIndexInChild == C.INDEX_UNSET ? C.INDEX_UNSET
				: getFirstPeriodIndexByChildIndex(childIndex) + periodIndexInChild;
		}

		private int getNextChildIndex(int childIndex) {
			return childIndex < childCount - 1 ? childIndex + 1 : C.INDEX_UNSET;
		}

		private int getPreviousChildIndex(int childIndex) {
			return childIndex > 0 ? childIndex - 1 : C.INDEX_UNSET;
		}
	}

	private static final class DeferredTimeline extends ForwardingTimeline {

		private static final Object DUMMY_ID = new Object();
		private static final Period period = new Period();
		private static final DummyTimeline dummyTimeline = new DummyTimeline();

		private final Object replacedId;

		DeferredTimeline() {
			this(dummyTimeline, /* replacedId= */ null);
		}

		private DeferredTimeline(Timeline timeline, Object replacedId) {
			super(timeline);
			this.replacedId = replacedId;
		}

		DeferredTimeline cloneWithNewTimeline(Timeline timeline) {
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

