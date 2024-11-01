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

	private TASWriter writer = new TASWriter();

	@Override
	public void frame(TASClient client) throws IOException {
		Inputs in = client.getInputs();
		int prev = 0;
		if ((in.pressed | in.released) != 0) {
			prev = client.previousInputMask();
		}
		System.out.print(writer.frame(prev, in));
		waitUntilNextFrame();
	}
}
