package iwltas;

import java.io.*;

public class StringReaderWithBreakpoint extends Reader {
	String str;
	int index;
	int endIndex;

	int breakpoint;
	Runnable onBreakpointReached;

	public StringReaderWithBreakpoint(String str, int start, int end, int breakpoint, Runnable event) {
		this.str = str;
		this.index = start;
		this.endIndex = end;
		this.breakpoint = breakpoint;
		this.onBreakpointReached = event;
	}

	public StringReaderWithBreakpoint(String str, int breakpoint, Runnable event) {
		this(str, 0, str.length(), breakpoint, event);
	}

	@Override
	public int read() {
		if (index == breakpoint) {
			onBreakpointReached.run();
		}
		if (index >= endIndex) {
			return -1;
		}
		return str.charAt(index++);
	}

	@Override
	public int read(char[] ch, int off, int len) {
		if (len == 0) {
			return 0;
		}
		int readEnd = index + len;
		if (breakpoint >= index && breakpoint < readEnd) {
			onBreakpointReached.run();
		}
		readEnd = Math.min(readEnd, endIndex);
		str.getChars(index, readEnd, ch, off);

		len = readEnd - index;
		index = readEnd;

		return len == 0 ? -1 : len;
	}

	@Override
	public void close() {
	}
}
