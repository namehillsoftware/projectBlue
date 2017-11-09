package com.namehillsoftware.handoff.promises.response;

public class ImmediateAction {
	public static <Resolution> ImmediateResponse<Resolution, Void> perform(ResponseAction<Resolution> responseAction) {
		return new VoidImmediateResponse<Resolution>(responseAction);
	}

	private static class VoidImmediateResponse<Resolution> implements ImmediateResponse<Resolution, Void> {
		private final ResponseAction<Resolution> responseAction;

		VoidImmediateResponse(ResponseAction<Resolution> responseAction) {
			this.responseAction = responseAction;
		}

		@Override
		public Void respond(Resolution resolution) throws Throwable {
			responseAction.perform(resolution);
			return null;
		}
	}
}
