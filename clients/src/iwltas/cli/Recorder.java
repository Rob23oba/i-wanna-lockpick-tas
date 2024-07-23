package iwltas.cli;

import java.io.*;
import java.util.*;

import iwltas.*;

public class Recorder extends WaitingTASFrameHandler {
	public static void main(String[] args) {
		Recorder rec = new Recorder();
		for (int i = 0; i < args.length;) {
			switch (args[i++]) {
			case "--help":
				System.err.println("""
				Usage: java iwltas.cli.Recorder [options]

				Options:
				--help:      Show this help message.
				--fps <fps>: Set the frames per second.
				""");
				System.exit(1);
				break;
			case "--fps":
				try {
					rec.frameRate = Integer.parseInt(args[i++]);
				} catch (Exception e) {
					System.err.println("Invalid argument for --fps, expected integer!");
					System.exit(1);
				}
				break;
			default:
				System.err.println("Invalid option " + args[i - 1] + ". Use --help for a help message.");
				System.exit(1);
				break;
			}
		}
		try (TASClient client = new TASClient()) {
			client.runBlocking(rec);
		} catch (Exception ex) {
			System.err.println("An error occurred: " + ex);
		}
	}

	/**
	 * The accumulated delay after the last input.
	 */
	private int recordDelay;

	@Override
	public void frame(TASClient client) throws IOException {
		Inputs in = client.getInputs();
		if ((in.pressed | in.released) == 0) {
			recordDelay++;
			waitUntilNextFrame();
			return;
		}
		StringBuilder b = new StringBuilder();
		if (recordDelay != 0) {
			b.append(" ").append(recordDelay).append(" ");
			recordDelay = 0;
		}
		int prev = client.previousInputMask();
		for (int i = 0; i < 17; i++) {
			int mask = 1 << i;
			if ((mask & in.pressed) != 0 && (mask & in.released) == 0) {
				// Was pressed, not released
				b.append(Inputs.TAS_STRING.charAt(i));
			} else if ((mask & in.pressed) == 0 && (mask & in.released) != 0) {
				// Was released, not pressed
				b.append(Inputs.TAS_STRING_OFF.charAt(i));
			} else if ((mask & in.pressed) != 0 && (mask & in.released) != 0) {
				// Was pressed and released
				if ((mask & prev) != 0) {
					b.append(Inputs.TAS_STRING_OFF.charAt(i));
					b.append(Inputs.TAS_STRING.charAt(i));
				} else {
					b.append(Inputs.TAS_STRING.charAt(i));
					b.append(Inputs.TAS_STRING_OFF.charAt(i));
				}
				if ((mask & (prev ^ in.held)) != 0) {
					if ((mask & prev) != 0) {
						b.append(Inputs.TAS_STRING_OFF.charAt(i));
					} else {
						b.append(Inputs.TAS_STRING.charAt(i));
					}
				}
			}
		}
		System.out.print(b);

		recordDelay++;
		waitUntilNextFrame();
	}
}
