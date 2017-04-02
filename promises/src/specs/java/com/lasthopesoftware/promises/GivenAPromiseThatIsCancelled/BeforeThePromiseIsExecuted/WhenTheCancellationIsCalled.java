package com.lasthopesoftware.promises.GivenAPromiseThatIsCancelled.BeforeThePromiseIsExecuted;

import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.runnables.TwoParameterAction;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by david on 10/17/16.
 */

public class WhenTheCancellationIsCalled {

	private static Throwable thrownException;
	private static Throwable caughtException;

	@BeforeClass
	public static void before() {
		thrownException = new Exception();
		final ExternallyResolvableTask<String> resolvableTask = new ExternallyResolvableTask<>();
		final Promise<String> promise = new Promise<>(resolvableTask);

		Promise<Object> cancellablePromise = promise.then(
			(result, resolve, reject, onCancelled) -> onCancelled.runWith(() -> reject.sendRejection(thrownException)));

		cancellablePromise.error((exception, onCancelled) -> caughtException = exception);

		cancellablePromise.cancel();

		resolvableTask.resolve("");
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		assertThat(caughtException).isEqualTo(thrownException);
	}

	private static class ExternallyResolvableTask<TResult> implements TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> {

		private IResolvedPromise<TResult> resolve;

		public void resolve(TResult resolution) {
			if (resolve != null)
				resolve.sendResolution(resolution);
		}

		@Override
		public void runWith(IResolvedPromise<TResult> parameterOne, IRejectedPromise parameterTwo) {
			resolve = parameterOne;
		}
	}
}
