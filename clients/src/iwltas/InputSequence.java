package iwltas;

import java.util.*;

public class InputSequence {
	private static final int HASH_MULTIPLIER = 31;
	private static final int HASH_MULTIPLIER_INVERSE = 0xBDEF7BDF;

	private static final int INITIAL_HASH = 0x12345678;

	long[] seq;
	int size;

	private int hash;

	public InputSequence() {
		seq = new long[16];
		hash = INITIAL_HASH;
	}

	private InputSequence(long[] seq, int hash) {
		this.seq = seq;
		this.size = seq.length;
		this.hash = hash;
	}

	public void add(long value) {
		if (size >= seq.length) {
			seq = Arrays.copyOf(seq, size * 2);
		}
		value &= 0x1FFFF_1FFFF_1FFFFL;
		hash = hash * HASH_MULTIPLIER + Long.hashCode(value);
		seq[size++] = value;
	}

	public void pop() {
		long lastValue = seq[--size];
		hash = (hash - Long.hashCode(lastValue)) * HASH_MULTIPLIER_INVERSE;
	}

	public void clear() {
		size = 0;
		hash = INITIAL_HASH;
	}

	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof InputSequence other)) {
			return false;
		}
		if (hash != other.hash) {
			return false;
		}
		return Arrays.equals(seq, 0, size, other.seq, 0, other.size);
	}

	public boolean startsWith(InputSequence other) {
		return size >= other.size && Arrays.equals(seq, 0, other.size, other.seq, 0, other.size);
	}

	public int hashCode() {
		return hash;
	}

	public InputSequence copy() {
		return new InputSequence(Arrays.copyOf(seq, size), hash);
	}
}
