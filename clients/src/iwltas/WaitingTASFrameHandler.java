package iwltas;

/**
 * Represents a TAS frame handler with the ability to run the frames at a consistent rate.
 */
public abstract class WaitingTASFrameHandler implements TASFrameHandler {
	protected long lastFrameTime = System.nanoTime();

	/**
	 * The frame rate (FPS) the frame handler should run at.
	 */
	public int frameRate = 50;

	/**
	 * Waits until the next frame. Usually waits around {@code 1.0 / frameRate} seconds.
	 *
	 * @return whether the waiting was interrupted.
	 */
	public boolean waitUntilNextFrame() {
		lastFrameTime += 1_000_000_000L / frameRate;

		long currentTime = System.nanoTime();
		long wait = lastFrameTime - currentTime;
		if (wait < 0) {
			lastFrameTime = currentTime;
			return false;
		}
		try {
			Thread.sleep(wait / 1_000_000L);
			return false;
		} catch (InterruptedException ex) {
			return true;
		}
	}
}
