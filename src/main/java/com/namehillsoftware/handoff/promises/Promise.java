package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.SingleMessageBroadcaster;
import com.namehillsoftware.handoff.promises.response.EventualAction;
import com.namehillsoftware.handoff.promises.response.ImmediateAction;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;
import com.namehillsoftware.handoff.rejections.UnhandledRejectionsReceiver;

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

	public final Promise<Resolution> must(ImmediateAction onAny) {
		return then(new AlwaysImmediateResponse<>(onAny));
	}

	public final Promise<Resolution> inevitably(EventualAction onAny) {
	    return eventually(r -> onAny.respond().then(v -> r), e -> onAny.respond().then(v -> {
	        throw e;
        }));
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

	public static class Rejections {
		public static void setUnhandledRejectionsReceiver(UnhandledRejectionsReceiver receiver) {
			SingleMessageBroadcaster.setUnhandledRejectionsReceiver(receiver);
		}
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
