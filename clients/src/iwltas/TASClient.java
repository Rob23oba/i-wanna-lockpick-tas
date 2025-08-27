package iwltas;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * A client that supports the text-based IWL TAS protocol.
 */
public class TASClient implements Closeable {
	/**
	 * The socket connected to the server.
	 */
	private Socket socket;

	/**
	 * The socket's input stream as a {@link BufferedReader}.
	 */
	private BufferedReader in;

	/**
	 * The socket's output stream as a {@link PrintStream}.
	 */
	private PrintStream out;

	/**
	 * Whether the server is in blocking mode.
	 */
	private boolean blocking;

	/**
	 * Initializes the client and connects it to {@code localhost:13785}.
	 */
	public TASClient() throws IOException {
		this("localhost", 13785);
	}

	/**
	 * Initializes the client and connects it to the given host and port.
	 *
	 * @param host the host to connect to.
	 * @param port the port to connect to.
	 */
	public TASClient(String host, int port) throws IOException {
		this.socket = new Socket(host, port);
		this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.out = new PrintStream(socket.getOutputStream());
	}

	/**
	 * Sends a raw message to the server.
	 *
	 * @param str the message.
	 */
	public void send(String str) throws IOException {
		out.println(str);
		//System.out.println("[Client] " + str);
	}

	/**
	 * Waits for a response from the server and returns it.
	 *
	 * @return the response.
	 */
	public String get() throws IOException {
		while (true) {
			String str = in.readLine();
			if (str == null) {
				throw new EOFException("No line received");
			}
			if (str.startsWith("$")) {
				//System.out.println(str);
				// debug message
				continue;
			}
			return str;
		}
	}

	/**
	 * Runs a {@link TASFrameHandler} in blocking mode.
	 */
	public void runBlocking(TASFrameHandler f) throws IOException {
		send("block");
		blocking = true;
		while (blocking) {
			f.frame(this);
			doInputs();
			send("end_frame");

                        if (!blocking) break;

			String abc = get();
			if (!abc.equals("frame")) {
				send("unblock");
				throw new IOException("Received non-frame response: " + abc);
			}
		}
	}

	/**
	 * Puts the server out of blocking mode. Will terminate {@link #runBlocking}.
         */
	public void stopBlocking() throws IOException {
		if (blocking) {
			send("unblock");
		}
		blocking = false;
	}

	/**
	 * All currently queued inputs.
	 */
	public Inputs currentInputs = new Inputs();

	/**
	 * Queues pressing all buttons in the provided button mask.
	 *
	 * @param mask the buttons to press. Use the constants from {@link Inputs}.
         */
	public void press(int mask) {
		currentInputs.held |= mask;
		currentInputs.heldMask |= mask;
		currentInputs.pressed |= mask;
		currentInputs.pressedMask |= mask;
	}

	/**
	 * Queues releasing all buttons in the provided button mask.
	 *
	 * @param mask the buttons to release. Use the constants from {@link Inputs}.
         */
	public void release(int mask) {
		currentInputs.held &= ~mask;
		currentInputs.heldMask |= mask;
		currentInputs.released |= mask;
		currentInputs.releasedMask |= mask;
	}

	/**
	 * Sends any queued inputs to the server.
	 */
	public void doInputs() throws IOException {
		doInputs(currentInputs);
		currentInputs.reset();
	}

	/**
	 * Sends the provided set of inputs to the server.
	 *
	 * @param inputs the inputs to send to the server.
	 */
	public void doInputs(Inputs inputs) throws IOException {
		if (inputs.isEmpty()) {
			return;
		}
		send("set_inputs");
		send(inputs.heldString());
		send(inputs.pressedString());
		send(inputs.releasedString());
	}

	/**
	 * Sends the provided set of inputs to the server.
	 *
	 * @param held mask of all buttons that should be held down.
	 * @param pressed mask of all buttons that should be pressed.
	 * @param released mask of all buttons that should be released.
	 */
	public void doInputs(int held, int pressed, int released) throws IOException {
		if (held == 0 && pressed == 0 && released == 0) {
			return;
		}
		send("set_inputs");
		send(Inputs.toString(held, Inputs.BUTTON_MASK));
		send(Inputs.toString(pressed, Inputs.BUTTON_MASK));
		send(Inputs.toString(released, Inputs.BUTTON_MASK));
	}

	/**
	 * Returns the currently active inputs.
	 */
	public Inputs getInputs() throws IOException {
		send("get_inputs");
		return new Inputs(get(), get(), get());
	}

	/**
	 * Returns a mask of the input in the previous frame.
	 */
        public int previousInputMask() throws IOException {
		send("curprev");

                int prev = Integer.parseInt(get());
                get(); // current
		return prev;
	}

	private static String[] splitPrefix(String str) {
		int eq = str.indexOf('=');
		return new String[] { str.substring(0, eq), str.substring(eq + 1) };
	}

	/**
	 * Returns information about the current location.
	 */
	public LocationInformation getLocation() throws IOException {
		LocationInformation info = new LocationInformation();
		send("location");
		String struct = get();
		for (String s : struct.split(" ")) {
			String[] split = splitPrefix(s);
			switch (split[0]) {
			case "room" -> info.room = Integer.parseInt(split[1]);
			case "x" -> {
				info.hasPlayer = true;
				info.x = Double.parseDouble(split[1]);
			}
			case "y" -> info.y = Double.parseDouble(split[1]);
			case "hspeed" -> info.hspeed = Double.parseDouble(split[1]);
			case "vspeed" -> info.vspeed = Double.parseDouble(split[1]);
			}
		}
		return info;
	}

	/**
	 * Returns the list of obstacles (objects derived from objBlock) in the level.
	 */
	public List<Obstacle> getObstacles() throws IOException {
		List<Obstacle> obstacles = new ArrayList<>();
		send("obstacles");
		while (true) {
			String s = get();
			if ("end".equals(s)) {
				return obstacles;
			}
			String[] split = s.split(" ");
			Obstacle o = new Obstacle();
			o.object_index = Integer.parseInt(split[0]);
			o.x = Float.parseFloat(split[1]);
			o.y = Float.parseFloat(split[2]);
			o.xscale = Float.parseFloat(split[3]);
			o.yscale = Float.parseFloat(split[4]);
			o.bbx = Integer.parseInt(split[5]);
			o.bby = Integer.parseInt(split[6]);
			o.bbwidth = Integer.parseInt(split[7]);
			o.bbheight = Integer.parseInt(split[8]);
			obstacles.add(o);
		}
	}

	/**
	 * Returns the list of other simple instances (e.g. keys, goals) in the level.
	 */
	public List<SimpleInstanceRecord> getOtherInstances() throws IOException {
		List<SimpleInstanceRecord> list = new ArrayList<>();
		send("other_stuff");
		while (true) {
			String s = get();
			if ("end".equals(s)) {
				return list;
			}
			String[] split = s.split(" ");
			SimpleInstanceRecord rec = new SimpleInstanceRecord();
			rec.object_index = Integer.parseInt(split[0]);
			rec.x = Float.parseFloat(split[1]);
			rec.y = Float.parseFloat(split[2]);
			rec.mask_index = Integer.parseInt(split[3]);
			list.add(rec);
		}
	}

	/**
	 * Returns a two-dimensional double array representing a save state that can be loaded using {@link #loadSaveState}.
	 */
	public double[][] getSaveState() throws IOException {
		send("savestate");
		List<double[]> list = new ArrayList<>();
		while (true) {
			String s = get();
			if ("end".equals(s)) {
				return list.toArray(new double[0][]);
			}
			String[] split = s.split(" ");
			double[] d = new double[split.length];
			for (int i = 0; i < split.length; i++) {
				d[i] = Double.parseDouble(split[i]);
			}
			list.add(d);
		}
	}

	/**
	 * Loads back a save state as returned by {@link #getSaveState}.
	 *
	 * @param saveState the save state data.
	 */
	public void loadSaveState(double[][] saveState) throws IOException {
		String saveStateString = "";
		for (int i = 0; i < saveState.length; i++) {
			if (i > 0) saveStateString += "\n";
			double[] ds = saveState[i];
			for (int j = 0; j < ds.length; j++) {
				if (j > 0) saveStateString += " ";
				double d = ds[j];
				if ((int) d == d) {
					saveStateString += (int) d;
				} else {
					saveStateString += d;
				}
			}
		}
		send("load_savestate\n" + saveStateString);
	}

	/**
	 * Closes the client and the underlying socket.
	 */
	public void close() throws IOException {
		socket.close();
	}
}
