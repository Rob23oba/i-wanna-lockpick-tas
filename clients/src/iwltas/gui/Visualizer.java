package iwltas.gui;

import java.io.*;
import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import iwltas.*;

public class Visualizer extends WaitingTASFrameHandler {
	public static void main(String[] args) throws IOException {
		Visualizer v = new Visualizer();

		VisualizerPanel panel = new VisualizerPanel();
		panel.v = v;
		v.panel = panel;

		panel.setFocusable(true);
		panel.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent ev) {
				v.inputs.offer(ev);
			}

			public void keyReleased(KeyEvent ev) {
				v.inputs.offer(ev);
			}
		});
		panel.setPreferredSize(new Dimension(800, 608));

		JFrame frame = new JFrame();
		frame.add(panel);
		frame.pack();
		frame.setVisible(true);

		try (TASClient client = new TASClient()) {
			client.runBlocking(v);
		} finally {
			frame.setVisible(false);
			frame.dispose();
		}
	}

	public VisualizerPanel panel;
	public Queue<KeyEvent> inputs = new ArrayDeque<>();

	public final int[] binds = {
		KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN,
		KeyEvent.VK_SPACE, KeyEvent.VK_Z, KeyEvent.VK_R, KeyEvent.VK_S,
		KeyEvent.VK_ESCAPE, KeyEvent.VK_C, KeyEvent.VK_X, KeyEvent.VK_BACK_SPACE,
		KeyEvent.VK_W, KeyEvent.VK_A, KeyEvent.VK_CONTROL, KeyEvent.VK_E, KeyEvent.VK_SHIFT,
	};

	List<Obstacle> obstacles = new ArrayList<>();
	LocationInformation location = new LocationInformation();

	@Override
	public void frame(TASClient client) throws IOException {
		synchronized (inputs) {
			while (!inputs.isEmpty()) {
				KeyEvent ev = inputs.poll();
				int keyCode = ev.getKeyCode();
				for (int i = 0; i < binds.length; i++) {
					if (binds[i] == keyCode) {
						if (ev.getID() == KeyEvent.KEY_PRESSED) {
							client.press(1 << i);
						} else {
							client.release(1 << i);
						}
					}
				}
			}
		}
		obstacles = client.getObstacles();
		location = client.getLocation();
		panel.repaint();

		waitUntilNextFrame();
	}
}
