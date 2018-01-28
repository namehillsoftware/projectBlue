package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.SingleMessageBroadcaster;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;

import java.util.Arrays;
import java.util.Collection;

public class Promise<Resolution> {

	private final SingleMessageBroadcaster<Resolution> singleMessageBroadcaster;

	public Promise(MessengerOperator<Resolution> messengerOperator) {
		this();
		messengerOperator.send(singleMessageBroadcaster);
	}

	public Promise(Resolution passThroughResult) {
		this();
		singleMessageBroadcaster.sendResolution(passThroughResult);
	}

	private Promise() {
		this(new SingleMessageBroadcaster<>());
	}

	private Promise(SingleMessageBroadcaster<Resolution> singleMessageBroadcaster) {
		this.singleMessageBroadcaster = singleMessageBroadcaster;
	}

	public Promise(Throwable rejection) {
		this();
		singleMessageBroadcaster.sendRejection(rejection);
	}

	public final void cancel() {
		singleMessageBroadcaster.cancel();
	}

	private <NewResolution> Promise<NewResolution> then(ResponseRoutingMessenger<Resolution, NewResolution> onFulfilled) {
		singleMessageBroadcaster.awaitResolution(onFulfilled);

		return new Promise<>(onFulfilled);
	}

	public final <NewResolution> Promise<NewResolution> then(ImmediateResponse<Resolution, NewResolution> onFulfilled) {
		return then(new FulfilledResponseExecutor<>(onFulfilled));
	}

	public final <NewResolution> Promise<NewResolution> then(ImmediateResponse<Resolution, NewResolution> onFulfilled, ImmediateResponse<Throwable, NewResolution> onRejected) {
		return then(new ResponseExecutor<>(onFulfilled, onRejected));
	}

	public final <NewResolution> Promise<NewResolution> eventually(PromisedResponse<Resolution, NewResolution> onFulfilled) {
		return then(new PromisedResolutionResponseMessenger<>(onFulfilled));
	}

	public final <NewResolution> Promise<NewResolution> eventually(PromisedResponse<Resolution, NewResolution> onFulfilled, PromisedResponse<Throwable, NewResolution> onRejected) {
		return then(new PromisedGuaranteedResponseMessenger<>(onFulfilled, onRejected));
	}

	public final <NewRejection> Promise<NewRejection> excuse(ImmediateResponse<Throwable, NewRejection> onRejected) {
		return then(new RejectedResponseExecutor<>(onRejected));
	}

	public static <Resolution> Promise<Resolution> empty() {
		return new Promise<>((Resolution) null);
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
		return new Promise<>(new Resolutions.FirstPromiseResolver<>(promises));
	}
}
