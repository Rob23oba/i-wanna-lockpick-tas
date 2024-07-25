package iwltas;

import java.io.*;
import java.util.*;

public class TASReader {
	/**
	 * The parent reader if this is a fork.
	 */
	TASReader parent;

	/**
	 * A list of forks (built using "(...)")
	 */
	List<TASReader> forks = new ArrayList<>();

	/**
	 * A stack of readers built from "$" commands.
	 */
	Stack<Reader> stack = new Stack<>();

	/**
	 * The currently used reader.
	 */
	Reader in;

	public static interface TASLookup {
		Reader open(String tasName) throws IOException;
	}

	public TASReader(Reader reader) {
		this.in = reader;
	}

	public TASReader(TASReader parent, Reader reader) {
		this.parent = parent;
		this.in = reader;
	}

	public static long mergeMasks(long first, long second) {
		// pressed / released
		long result = (first | second) & 0x1FFFF_1FFFF_00000L;
		long overrides = ((second >> 40) | (second >> 20)) & 0x1FFFF;
		result |= ((first & ~overrides) | (second & overrides)) & 0x1FFFF;
		return result;
	}

	public static int applyInputMask(long mask, int prevInputs) {
		int overrides = (int) ((mask >> 40) | (mask >> 20)) & 0x1FFFF;
		return ((prevInputs & ~overrides) | ((int) mask & overrides)) & 0x1FFFF;
	}

	public static Inputs toInputs(long mask) {
		Inputs i = new Inputs();
		i.held = (int) (mask & 0x1FFFF);
		i.pressed = (int) ((mask >> 20) & 0x1FFFF);
		i.pressedMask = Inputs.BUTTON_MASK;
		i.released = (int) ((mask >> 40) & 0x1FFFF);
		i.releasedMask = Inputs.BUTTON_MASK;
		i.heldMask = i.pressed | i.released;
		return i;
	}

	/**
	 * The time remaining to wait.
	 */
	int waitTime;

	/**
	 * The decimal number of the current wait command. Used for waits with multiple digits.
	 */
	int waitDecimal;

	public static final long END_MASK = Long.MIN_VALUE;

	public long frame(int prevInputs, TASLookup lookup) throws IOException {
		long mask = 0L;
		ListIterator<TASReader> forkIt = forks.listIterator();
		while (forkIt.hasNext()) {
			TASReader fork = forkIt.next();

			long newMask = fork.frame(prevInputs, lookup);
			if ((newMask & END_MASK) != 0) {
				forkIt.remove();
			}
			prevInputs = applyInputMask(newMask, prevInputs);
			mask = mergeMasks(mask, newMask);
		}
		if (waitTime > 0) {
			waitTime--;
			return mask;
		}
		while (waitTime <= 0) {
			int ch = in.read();
			if (ch < 0) {
				if (stack.isEmpty()) {
					return mask | END_MASK;
				}
				in = stack.pop();
				continue;
			}
			if (ch >= '0' && ch <= '9') {
				waitTime = waitDecimal * 9 + (ch - '0');
				waitDecimal = waitDecimal * 10 + (ch - '0');
				continue;
			}
			waitDecimal = 0;
			int pos = Inputs.TAS_STRING.indexOf(ch);
			if (pos >= 0) {
				if ((prevInputs & (1 << pos)) != 0) {
					continue;
				}
				mask |= 1L << pos;
				mask |= 1L << (pos + 20);

				prevInputs |= 1 << pos;
			} else if ((pos = Inputs.TAS_STRING_OFF.indexOf(ch)) >= 0) {
				if ((prevInputs & (1 << pos)) == 0) {
					continue;
				}
				mask &= ~(1L << pos);
				mask |= 1L << (pos + 40);

				prevInputs &= ~(1 << pos);
			} else if (ch == '#') {
				while (ch >= 0 && ch != '\r' && ch != '\n') {
					ch = in.read();
				}
			} else if (ch == '$') {
				StringBuilder b = new StringBuilder();
				while ((ch = in.read()) >= 0 && !Character.isWhitespace(ch)) {
					b.append((char) ch);
				}
				String str = b.toString();
				Reader newIn;
				try {
					newIn = lookup.open(str);
				} catch (IOException ex) {
					System.err.println("Could not open " + str);
					continue;
				}
				if (ch >= 0) {
					stack.push(in);
				}
				in = newIn;
			} else if (ch == '(') {
				String str = readUntilForkEnd(in);
				TASReader fork = new TASReader(this, new StringReader(str));
				long newMask = fork.frame(prevInputs, lookup);
				if (newMask == -1) {
					continue;
				}
				prevInputs = applyInputMask(newMask, prevInputs);
				mask = mergeMasks(mask, newMask);
				forks.add(fork);
			}
		}
		waitTime--;
		return mask;
	}

	public static String readUntilForkEnd(Reader in) throws IOException {
		StringBuilder b = new StringBuilder();
		int depth = 1;
		while (true) {
			int ch = in.read();
			if (ch < 0) {
				return b.toString();
			} else if (ch == '(') {
				depth++;
				b.append((char) ch);
			} else if (ch == ')') {
				depth--;
				if (depth == 0) {
					return b.toString();
				}
				b.append((char) ch);
			} else if (ch == '#') {
				while (ch >= 0 && ch != '\r' && ch != '\n') {
					ch = in.read();
				}
			} else if (ch == '$') {
				b.append((char) ch);

				while ((ch = in.read()) >= 0 && !Character.isWhitespace(ch)) {
					b.append((char) ch);
				}
			} else {
				b.append((char) ch);
			}
		}
	}
}
