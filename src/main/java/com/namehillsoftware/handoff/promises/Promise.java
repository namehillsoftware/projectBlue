package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.SingleMessageBroadcaster;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;

import java.util.Arrays;
import java.util.Collection;

public class Promise<Resolution> extends SingleMessageBroadcaster<Resolution> {

	public Promise(MessengerOperator<Resolution> messengerOperator) {
		messengerOperator.send(new PromiseMessenger());
	}

	public Promise(Resolution passThroughResult) {
		resolve(passThroughResult);
	}

	public Promise(Throwable rejection) {
		reject(rejection);
	}

	public Promise() {}

	private <NewResolution> Promise<NewResolution> then(PromiseResponse<Resolution, NewResolution> onFulfilled) {
		awaitResolution(onFulfilled);

		return onFulfilled;
	}

	public final <NewResolution> Promise<NewResolution> then(ImmediateResponse<Resolution, NewResolution> onFulfilled) {
		return then(new PromiseImmediateResponse<>(onFulfilled));
	}

	public final <NewResolution> Promise<NewResolution> then(ImmediateResponse<Resolution, NewResolution> onFulfilled, ImmediateResponse<Throwable, NewResolution> onRejected) {
		return then(new PromiseImmediateResponse<>(onFulfilled, onRejected));
	}

	public final <NewResolution> Promise<NewResolution> eventually(PromisedResponse<Resolution, NewResolution> onFulfilled) {
		return then(new PromisedEventualResponse<>(onFulfilled));
	}

	public final <NewResolution> Promise<NewResolution> eventually(PromisedResponse<Resolution, NewResolution> onFulfilled, PromisedResponse<Throwable, NewResolution> onRejected) {
		return then(new PromisedEventualResponse<>(onFulfilled, onRejected));
	}

	public final <NewRejection> Promise<NewRejection> excuse(ImmediateResponse<Throwable, NewRejection> onRejected) {
		return then(new RejectedResponsePromise<>(onRejected));
	}

	@SuppressWarnings("unchecked")
	public static <Resolution> Promise<Resolution> empty() {
		return LazyEmptyPromiseHolder.emptyPromiseInstance;
	}

	@SafeVarargs
	public static <Resolution> Promise<Collection<Resolution>> whenAll(Promise<Resolution>... promises) {
		return whenAll(Arrays.asList(promises));
	}

	public static <Resolution> Promise<Collection<Resolution>> whenAll(Collection<Promise<Resolution>> promises) {
		return new Promise<>(new Resolutions.AggregatePromiseResolver<>(promises));
	}

	@SafeVarargs
	public static <Resolution> Promise<Resolution> whenAny(Promise<Resolution>... promises) {
		return whenAny(Arrays.asList(promises));
	}

	public static <Resolution> Promise<Resolution> whenAny(Collection<Promise<Resolution>> promises) {
		return new Resolutions.HonorFirstPromise<>(promises);
	}

	private static class LazyEmptyPromiseHolder {
		private static final Promise emptyPromiseInstance = new Promise<>((Object) null);
	}

	private class PromiseMessenger implements Messenger<Resolution> {

		@Override
		public void sendResolution(Resolution resolution) {
			resolve(resolution);
		}

		@Override
		public void sendRejection(Throwable error) {
			reject(error);
		}

		@Override
		public void cancellationRequested(Runnable response) {
			respondToCancellation(response);
		}
	}
}
