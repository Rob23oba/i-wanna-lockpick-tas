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
	 * The time remaining to wait.
	 */
	private int waitTime;

	/**
	 * The decimal number of the current wait command. Used for waits with multiple digits.
	 */
	private int waitDecimal;

	/**
	 * A stack of readers built from "$" commands.
	 */
	private Stack<Reader> stack = new Stack<>();

	/**
	 * The currently used reader.
	 */
	private Reader in = new BufferedReader(new InputStreamReader(System.in));

	@Override
	public void frame(TASClient client) throws IOException {
		int prev = client.previousInputMask();
		if (waitTime > 0) {
			client.doInputs();
			if (logInputs) {
				System.out.println(client.getInputs());
			}
			waitTime--;
			waitUntilNextFrame();
			return;
		}
		while (waitTime <= 0) {
			int ch = in.read();
			if (ch < 0) {
				if (stack.isEmpty()) {
					client.stopBlocking();
					return;
				}
				in = stack.pop();
				continue;
			}
			if (ch >= '0' && ch <= '9') {
				waitTime = waitDecimal * 9 + (ch - '0');
				waitDecimal = waitDecimal * 10 + (ch - '0');
			} else {
				waitDecimal = 0;
				int pos = Inputs.TAS_STRING.indexOf(ch);
				if (pos >= 0) {
					if ((prev & (1 << pos)) != 0) {
						continue;
					}
					client.press(1 << pos);
					prev |= 1 << pos;
				} else if ((pos = Inputs.TAS_STRING_OFF.indexOf(ch)) >= 0) {
					if ((prev & (1 << pos)) == 0) {
						continue;
					}
					client.release(1 << pos);
					prev &= ~(1 << pos);
				} else if (ch == '#') {
					while (ch >= 0 && ch != '\r' && ch != '\n') {
						ch = in.read();
					}
				} else if (ch == '$') {
					String str = "";
					ch = in.read();
					while (ch >= 0 && !Character.isWhitespace(ch)) {
						str += (char) ch;
						ch = in.read();
					}
					String content;
					try (FileInputStream fin = new FileInputStream(new File(searchRoot, str + ".txt"))) {
						content = new String(fin.readAllBytes());
					} catch (IOException ex) {
						System.err.println(str + " could not be found. Use --lookup to specify the search path. --help for more information.");
						continue;
					}
					stack.push(in);
					in = new StringReader(content);
				}
			}
		}
		client.doInputs();
		if (logInputs) {
			System.out.println(client.getInputs());
		}
		waitTime--;
		waitUntilNextFrame();
	}
}
