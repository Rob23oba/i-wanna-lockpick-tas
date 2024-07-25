package iwltas.cli;

import java.io.*;
import java.util.*;

import iwltas.*;

public class Playback extends WaitingTASFrameHandler {
	public static void main(String[] args) {
		Playback p = new Playback();
		for (int i = 0; i < args.length;) {
			switch (args[i++]) {
			case "--help":
				System.err.println("""
				Usage: java iwltas.cli.Playback [options]

				Options:
				--help:         Show this help message.
				--fps <fps>:    Set the frames per second.
				--lookup <dir>: Specify the directory to lookup TAS files in (default: current directory).
				--log:          Enable logging the current inputs.
				""");
				System.exit(1);
				break;
			case "--fps":
				try {
					p.frameRate = Integer.parseInt(args[i++]);
				} catch (Exception e) {
					System.err.println("Invalid argument for --fps, expected integer!");
					System.exit(1);
				}
				break;
			case "--lookup":
				if (i < args.length) {
					p.searchRoot = new File(args[i++]);
				} else {
					System.err.println("--lookup requires an argument");
					System.exit(1);
				}
				break;
			case "--log":
				p.logInputs = true;
				break;
			default:
				System.err.println("Invalid option " + args[i - 1] + ". Use --help for a help message.");
				System.exit(1);
				break;
			}
		}
		try (TASClient client = new TASClient()) {
			client.runBlocking(p);
		} catch (Exception ex) {
			System.err.println("An error occurred: " + ex);
		}
	}

	/**
	 * Where to search TAS files for the "$" command.
	 */
	public File searchRoot = new File(".");

	/**
	 * Whether to log the current inputs during playback.
	 */
	public boolean logInputs;

	/**
	 * The TAS reader used to read inputs.
	 */
	private TASReader reader = new TASReader(new BufferedReader(new InputStreamReader(System.in)));

	@Override
	public void frame(TASClient client) throws IOException {
		int prev = client.previousInputMask();
		long mask = reader.frame(prev, str -> {
			try (FileInputStream fin = new FileInputStream(new File(searchRoot, str + ".txt"))) {
				return new StringReader(new String(fin.readAllBytes()).stripTrailing());
			} catch (IOException ex) {
				System.err.println(str + " could not be found. Use --lookup to specify the search path. --help for more information.");
				return new StringReader("");
			}
		});
		client.doInputs(TASReader.toInputs(mask));
		if (logInputs) {
			System.out.println(client.getInputs());
		}
		if ((mask & TASReader.END_MASK) != 0) {
			client.stopBlocking();
			return;
		}
		waitUntilNextFrame();
	}
}
