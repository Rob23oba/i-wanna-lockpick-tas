package iwltas;

import java.util.*;
import java.io.*;

public class SaveStateCache extends WaitingTASFrameHandler {
	Map<InputSequence, double[][]> saveStates = new HashMap<>();
	InputSequence currentSequence = new InputSequence();

	public volatile InputSequence target;
	boolean reachedTarget;

	public Thread th;

	int room;

	public void clear() {
		saveStates.clear();
		currentSequence.clear();
	}

	public void interrupt() {
		if (th != null) {
			lastFrameTime = System.nanoTime();
			reachedTarget = false;
			th.interrupt();
		}
	}

	public void frame(TASClient client) throws IOException {
		th = Thread.currentThread();

		InputSequence target;
		synchronized (this) {
			target = this.target;
		}
		LocationInformation info = client.getLocation();
		if (info.room != room) {
			// Purge cache
			clear();
			room = info.room;
		}
		if (target == null) {
			currentSequence.clear();
			frameRate = 50;
			waitUntilNextFrame();
			return;
		}
		double[][] saveState = client.getSaveState();
		int prevInputs = (int) saveState[0][0];
		if (currentSequence.isEmpty() && prevInputs != 0) {
			client.release(prevInputs);
			return;
		}
		if (saveStates.size() > 1000) {
			Iterator<InputSequence> it = saveStates.keySet().iterator();
			while (it.hasNext()) {
				InputSequence q = it.next();
				if (q.startsWith(currentSequence) || currentSequence.startsWith(q)) {
					continue;
				}
				it.remove();
				if (saveStates.size() <= 1000) {
					break;
				}
			}
		}
		saveStates.put(currentSequence, saveState);

		if (target.isEmpty()) {
			return;
		}
		InputSequence targetCopy = target.copy();
		double[][] ss = null;
		do {
			targetCopy.pop();
			if (targetCopy.equals(currentSequence)) {
				break;
			}
			ss = saveStates.get(targetCopy);
			if (ss != null) {
				break;
			}
		} while (!targetCopy.isEmpty());
		if (ss != null) {
			client.loadSaveState(ss);
		}
		long inputMask = target.seq[targetCopy.size];
		client.doInputs(TASReader.toInputs(inputMask));

		currentSequence = targetCopy;
		currentSequence.add(inputMask);

		Thread.interrupted(); // Clear interrupted flag

		if (currentSequence.size >= target.size) {
			if (!reachedTarget) {
				reachedTarget = true;
				return;
			}
			frameRate = 5;
			waitUntilNextFrame();
		}
	}
}
