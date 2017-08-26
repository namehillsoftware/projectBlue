package com.lasthopesoftware.messenger.promises.GivenAPromiseThatIsCancelled.BeforeThePromiseIsExecuted.AndThePromiseIsExecuted;

import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.MessengerTask;
import com.lasthopesoftware.messenger.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenTheCancellationIsCalled {

	private static final Throwable thrownException = new Exception();
	private static Throwable caughtException;

	@BeforeClass
	public static void before() {
		final ExternallyResolvableTask<String> resolvableTask = new ExternallyResolvableTask<>();
		final Promise<String> promise = new Promise<>(resolvableTask);

		final Promise<Object> cancellablePromise = promise.eventually((result) -> new Promise<>(messenger -> messenger.cancellationRequested(() -> messenger.sendRejection(thrownException))));

		cancellablePromise.excuse((exception) -> caughtException = exception);

		cancellablePromise.cancel();

		resolvableTask.resolve("");
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		assertThat(caughtException).isEqualTo(thrownException);
	}

	private static class ExternallyResolvableTask<TResult> implements MessengerTask<TResult> {

		private Messenger<TResult> resolve;

		public void resolve(TResult resolution) {
			if (resolve != null)
				resolve.sendResolution(resolution);
		}

		@Override
		public void execute(Messenger<TResult> messenger) {
			resolve = messenger;
		}
	}
}
