package iwltas;

import java.io.*;
import java.util.*;

public class TASWriter {
	/**
	 * The accumulated delay after the last input.
	 */
	public int recordDelay;

	/**
	 * Processes one frame and returns the inputs in the TAS format.
	 *
	 * @return the inputs in TAS format
	 */
	public String frame(int prevInputs, Inputs curInputs) throws IOException {
		if ((curInputs.pressed | curInputs.released) == 0) {
			recordDelay++;
			return "";
		}
		StringBuilder b = new StringBuilder();
		if (recordDelay != 0) {
			b.append(" ").append(recordDelay).append(" ");
			recordDelay = 0;
		}
		for (int i = 0; i < 17; i++) {
			int mask = 1 << i;
			if ((mask & curInputs.pressed) != 0 && (mask & curInputs.released) == 0) {
				// Was pressed, not released
				b.append(Inputs.TAS_STRING.charAt(i));
			} else if ((mask & curInputs.pressed) == 0 && (mask & curInputs.released) != 0) {
				// Was released, not pressed
				b.append(Inputs.TAS_STRING_OFF.charAt(i));
			} else if ((mask & curInputs.pressed) != 0 && (mask & curInputs.released) != 0) {
				// Was pressed and released
				if ((mask & prevInputs) != 0) {
					b.append(Inputs.TAS_STRING_OFF.charAt(i));
					b.append(Inputs.TAS_STRING.charAt(i));
				} else {
					b.append(Inputs.TAS_STRING.charAt(i));
					b.append(Inputs.TAS_STRING_OFF.charAt(i));
				}
				if ((mask & (prevInputs ^ curInputs.held)) != 0) {
					if ((mask & prevInputs) != 0) {
						b.append(Inputs.TAS_STRING_OFF.charAt(i));
					} else {
						b.append(Inputs.TAS_STRING.charAt(i));
					}
				}
			}
		}
		recordDelay++;
		return b.toString();
	}

	/**
	 * Indicates that the last frame() call was the last one and returns any leftover wait.
	 */
	public String end() throws IOException {
		if (recordDelay > 1) {
			return " " + (recordDelay - 1);
		}
		return "";
	}
}
