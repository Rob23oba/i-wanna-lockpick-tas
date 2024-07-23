package iwltas;

/**
 * Represents a set of input overrides.
 */
public class Inputs {
	// Different inputs
	public static final int LEFT = 1 << 0;
	public static final int RIGHT = 1 << 1;
	public static final int UP = 1 << 2;
	public static final int DOWN = 1 << 3;
	public static final int JUMP = 1 << 4;
	public static final int UNDO = 1 << 5;
	public static final int RESTART = 1 << 6;
	public static final int SKIP = 1 << 7;
	public static final int PAUSE = 1 << 8;
	public static final int CAMERA = 1 << 9;
	public static final int MASTER = 1 << 10;
	public static final int EXIT = 1 << 11;
	public static final int WARP = 1 << 12;
	public static final int SPECIAL = 1 << 13;
	public static final int RUNSWITCH = 1 << 14;
	public static final int RUN = 1 << 15;
	public static final int WALK = 1 << 16;

	/**
	 * The mask of all buttons.
	 */
	public static final int BUTTON_MASK = (1 << 17) - 1;

	public static final String TAS_STRING = "LRUDJZNSPCXBWAEFO";
	public static final String TAS_STRING_OFF = "lrudjznspcxbwaefo";

	public int held;
	public int heldMask;
	public int pressed;
	public int pressedMask;
	public int released;
	public int releasedMask;

	/**
	 * Constructs an empty input set.
	 */
	public Inputs() {
	}

	/**
	 * Constructs the input set based on the given held, pressed and released strings.
	 *
	 * This constructor is used for the protocol communication.
	 */
	public Inputs(String held, String pressed, String released) {
		long x;

		x = fromString(held);
		this.held = (int) x;
		this.heldMask = (int) (x >> 32);

		x = fromString(pressed);
		this.pressed = (int) x;
		this.pressedMask = (int) (x >> 32);

		x = fromString(released);
		this.released = (int) x;
		this.releasedMask = (int) (x >> 32);
	}

	/**
	 * Converts the held buttons mask into a string.
	 */
	public String heldString() {
		return toString(held, heldMask);
	}

	/**
	 * Converts the pressed buttons mask into a string.
	 */
	public String pressedString() {
		return toString(pressed, pressedMask);
	}

	/**
	 * Converts the released buttons mask into a string.
	 */
	public String releasedString() {
		return toString(released, releasedMask);
	}

	/**
	 * Converts the input set into a string.
	 * It contains the held, pressed and released masks in order separated by spaces.
	 */
	public String toString() {
		return heldString() + " " + pressedString() + " " + releasedString();
	}

	/**
	 * Resets the input mask.
	 */
	public void reset() {
		this.held = 0;
		this.heldMask = 0;
		this.pressed = 0;
		this.pressedMask = 0;
		this.released = 0;
		this.releasedMask = 0;
	}

	/**
	 * Returns whether or not the input set doesn't override any buttons.
	 *
	 * @return whether the input set is empty.
	 */
	public boolean isEmpty() {
		return ((heldMask | pressedMask | releasedMask) & BUTTON_MASK) == 0;
	}

	/**
	 * Returns a button mask and value represented by the string
	 * given the formula {@code on | (mask << 32)}.
	 *
	 * @param s the string to parse.
	 * @return {@code on | (mask << 32)}.
	 */
	public static long fromString(String s) {
		long mask = 0L;
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			int index = TAS_STRING.indexOf(ch);
			if (index >= 0) {
				mask |= 1L << index;
				mask |= 1L << (32 + index);
			} else {
				index = TAS_STRING_OFF.indexOf(ch);
				if (index >= 0) {
					mask &= ~(1L << index);
					mask |= 1L << (32 + index);
				}
			}
		}
		return mask;
	}

	/**
	 * Converts the given inputs values and mask into a string.
	 *
	 * @param x the mask of buttons considered "on".
	 * @param mask the mask of buttons that are selected.
	 */
	public static String toString(int x, int mask) {
		String s = "";
		for (int i = 0; i < 17; i++) {
			if ((mask & (1 << i)) != 0) {
				if ((x & (1 << i)) != 0) {
					s += TAS_STRING.charAt(i);
				} else {
					s += TAS_STRING_OFF.charAt(i);
				}
			}
		}
		return s;
	}
}
