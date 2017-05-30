package com.lasthopesoftware.promises.GivenAPromiseThatIsCancelled.BeforeThePromiseIsExecuted;

import com.lasthopesoftware.promises.Messenger;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.runnables.OneParameterAction;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenTheCancellationIsCalled {

	private static Throwable thrownException;
	private static Throwable caughtException;

	@BeforeClass
	public static void before() {
		thrownException = new Exception();
		final ExternallyResolvableTask<String> resolvableTask = new ExternallyResolvableTask<>();
		final Promise<String> promise = new Promise<>(resolvableTask);

		Promise<Object> cancellablePromise = promise.then(
			(result) -> new Promise<>((messenger) -> messenger.cancellationRequested(() -> messenger.sendRejection(thrownException))));

		cancellablePromise.error((exception, onCancelled) -> caughtException = exception);

		cancellablePromise.cancel();

		resolvableTask.resolve("");
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		assertThat(caughtException).isEqualTo(thrownException);
	}

	private static class ExternallyResolvableTask<TResult> implements OneParameterAction<Messenger<TResult>> {

		private Messenger<TResult> resolve;

		public void resolve(TResult resolution) {
			if (resolve != null)
				resolve.sendResolution(resolution);
		}

		@Override
		public void runWith(Messenger<TResult> messenger) {
			resolve = messenger;
		}
	}
}
