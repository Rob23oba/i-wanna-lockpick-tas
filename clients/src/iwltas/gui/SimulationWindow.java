package iwltas.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

import iwltas.*;

@SuppressWarnings("serial")
public class SimulationWindow extends JPanel {
	IWLPhysicsSimulation simul = new IWLPhysicsSimulation();
	List<Obstacle> obstacles;
	double[][] saveState;
	JFrame frame;

	public VisualizerPanel panel;
	public Queue<KeyEvent> inputs = new ArrayDeque<>();

	public final int[] binds = {
		KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN,
		KeyEvent.VK_SPACE, KeyEvent.VK_Z, KeyEvent.VK_R, KeyEvent.VK_S,
		KeyEvent.VK_ESCAPE, KeyEvent.VK_C, KeyEvent.VK_X, KeyEvent.VK_BACK_SPACE,
		KeyEvent.VK_W, KeyEvent.VK_A, KeyEvent.VK_CONTROL, KeyEvent.VK_E, KeyEvent.VK_SHIFT,
	};
	int inputMask;

	@SuppressWarnings("this-escape")
	public SimulationWindow() {
		setPreferredSize(new Dimension(800, 608));
	}

	public void doInputs() {
		inputMask &= IWLPhysicsSimulation.HOLD_MASK;
		while (!inputs.isEmpty()) {
			KeyEvent ev = inputs.poll();
			int code = ev.getKeyCode();
			if (ev.getID() == KeyEvent.KEY_PRESSED) {
				if (code == binds[0]) {
					inputMask |= IWLPhysicsSimulation.LEFT;
				} else if (code == binds[1]) {
					inputMask |= IWLPhysicsSimulation.RIGHT;
				} else if (code == binds[4]) {
					inputMask |= IWLPhysicsSimulation.JUMP_PRESSED;
				} else if (code == binds[14]) {
					inputMask |= IWLPhysicsSimulation.WALK;
				} else if (code == binds[16]) {
					inputMask |= IWLPhysicsSimulation.RUN;
				}
			} else {
				if (code == binds[0]) {
					inputMask &= ~IWLPhysicsSimulation.LEFT;
				} else if (code == binds[1]) {
					inputMask &= ~IWLPhysicsSimulation.RIGHT;
				} else if (code == binds[4]) {
					inputMask |= IWLPhysicsSimulation.JUMP_RELEASED;
				} else if (code == binds[14]) {
					inputMask &= ~IWLPhysicsSimulation.WALK;
				} else if (code == binds[16]) {
					inputMask &= ~IWLPhysicsSimulation.RUN;
				}
			}
		}
	}

	public void doInputs(Inputs in) {
		inputMask = 0;
		if ((in.held & Inputs.LEFT) != 0) {
			inputMask |= IWLPhysicsSimulation.LEFT;
		}
		if ((in.held & Inputs.RIGHT) != 0) {
			inputMask |= IWLPhysicsSimulation.RIGHT;
		}
		if ((in.held & Inputs.WALK) != 0) {
			inputMask |= IWLPhysicsSimulation.WALK;
		}
		if ((in.held & Inputs.RUN) != 0) {
			inputMask |= IWLPhysicsSimulation.RUN;
		}
		if ((in.pressed & Inputs.JUMP) != 0) {
			inputMask |= IWLPhysicsSimulation.JUMP_PRESSED;
		}
		if ((in.released & Inputs.JUMP) != 0) {
			inputMask |= IWLPhysicsSimulation.JUMP_RELEASED;
		}
	}

	public void tickSimulation() {
		simul.tick(inputMask);
		obstacles = simul.obstacles;
		int flags = 0;
		if (simul.djump) flags |= 1;
		saveState = new double[][] {
			{ 0, simul.playerX, simul.playerY, simul.playerSpeed, flags, 0, 0, 0, 0, 0, 0 }
		};
	}

	public void showFrame() {
		dontAccept = false;
		if (frame == null) {
			frame = new JFrame("IWL Simulation Window");
			frame.add(this);
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			/*
			frame.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent ev) {
					inputs.offer(ev);
				}

				public void keyReleased(KeyEvent ev) {
					inputs.offer(ev);
				}
			});
			javax.swing.Timer timer = new javax.swing.Timer(20, ev -> {
				doInputs();
				tickSimulation();
				repaint();
			});
			timer.start();
			*/
			frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent ev) {
					frame = null;
					//timer.stop();
				}
			});
		}
		frame.setVisible(true);
	}

	public static Color gridColor(int coord) {
		coord = coord & -coord & 31; // lowest set bit < 32
		return switch (coord) {
		case 1 -> new Color(0, 0, 0, 8); // coord odd
		case 2 -> new Color(0, 0, 0, 12); // coord even
		case 4 -> new Color(0, 0, 0, 16); // coord divisible by 4
		case 8 -> new Color(0, 0, 0, 24); // coord divisible by 8
		case 16 -> new Color(0, 0, 0, 32); // coord divisible by 16
		default -> new Color(0, 0, 0, 48); // coord divisible by 32
		};
	}

	public static int gridCacheScale = -1;
	public static BufferedImage gridCache = null;

	public static void drawGrid(Graphics g, int diffX, int diffY, int w, int h, int scale) {
		if (scale != gridCacheScale) {
			gridCacheScale = scale;
			gridCache = new BufferedImage(scale * 64, scale * 64, BufferedImage.TYPE_INT_ARGB);
			Graphics gr = gridCache.createGraphics();
			for (int x = 0; x <= 64; x++) {
				gr.setColor(gridColor(x));
				gr.drawLine(x * scale + diffX, 0, x * scale + diffX, h);
			}
			for (int y = 0; y <= 64; y++) {
				gr.setColor(gridColor(y));
				gr.drawLine(0, y * scale + diffY, w, y * scale + diffY);
			}
		}
		int firstX = -Math.floorDiv(diffX, scale * 64); // = ceil(-diffX / (scale * 64))
		int firstY = -Math.floorDiv(diffY, scale * 64);
		int lastX = Math.floorDiv(w - diffX, scale * 64);
		int lastY = Math.floorDiv(h - diffY, scale * 64);
		for (int x = firstX; x <= lastX; x++) {
			for (int y = firstY; y <= lastY; y++) {
				g.drawImage(gridCache, diffX + x * scale * 64, diffY + y * scale * 64, null);
			}
		}
/*
		int firstX = -Math.floorDiv(diffX, scale); // = ceil(-diffX / scale)
		int firstY = -Math.floorDiv(diffY, scale);
		int lastX = Math.floorDiv(w - diffX, scale);
		int lastY = Math.floorDiv(h - diffY, scale);
		for (int x = firstX; x <= lastX; x++) {
			g.setColor(gridColor(x));
			g.drawLine(x * scale + diffX, 0, x * scale + diffX, h);
		}
		for (int y = firstY; y <= lastY; y++) {
			g.setColor(gridColor(y));
			g.drawLine(0, y * scale + diffY, w, y * scale + diffY);
		}
*/
	}

	public static void drawCenteredTextWithBackground(Graphics g, Color fg, Color bg, int x, int y, String text, int maxLen) {
		String[] lines = text.lines().toArray(String[]::new);

		FontMetrics met = g.getFontMetrics();
		int height = met.getHeight();
		int ascent = met.getAscent();
		y -= lines.length * height / 2;
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			int width = met.stringWidth(line);
			if (width > maxLen) {
				int index = 0;
				int maxPossibleIndex = 0;
				while (index < line.length()) {
					int newIndex = line.indexOf(' ', index);
					if (newIndex < 0) {
						newIndex = line.length();
					}
					int ww = met.stringWidth(line.substring(0, newIndex));
					if (ww > maxLen) {
						if (index == 0) {
							maxPossibleIndex = newIndex;
							width = ww;
						}
						break;
					}
					maxPossibleIndex = newIndex;
					width = ww;
					index = newIndex + 1;
				}
				String rem = line.substring(maxPossibleIndex).stripLeading();
				lines[i] = rem;
				i--;
				line = line.substring(0, maxPossibleIndex);
			}
			int leftX = x - width / 2;
			g.setColor(bg);
			g.fillRect(leftX, y, width, height);
			g.setColor(fg);
			g.drawString(line, leftX, y + ascent);
			y += height;
		}
	}

	private static final String[] flagNames = {
		"DJp", "Frz", "OnP", "RnS", "IMd", "AuR", "AuG", "AuB", "RcJ", "AnN"
	};

	public synchronized void paintComponent(Graphics g) {
		int w = getWidth();
		int h = getHeight();
		g.setColor(Color.white);
		g.fillRect(0, 0, w, h);
		if (saveState == null) return;
		int scale = 8;
		int playerX = (int) Math.rint(saveState[0][1] - 5);
		int playerY = (int) Math.rint(saveState[0][2] - 12);

		// make sure player is centered
		int diffX = (w - scale * 11) / 2 - scale * playerX;
		int diffY = (h - scale * 21) / 2 - scale * playerY;

		for (Obstacle o : obstacles) {
			g.setColor(VisualizerPanel.getColor(o.object_index));
			g.fillRect(o.bbx * scale + diffX, o.bby * scale + diffY,
				o.bbwidth * scale, o.bbheight * scale);
		}

		Color playerColor = VisualizerPanel.getColor(19);
		g.setColor(playerColor); // objPlayer
		g.fillRect(playerX * scale + diffX, playerY * scale + diffY, 11 * scale, 21 * scale);

		drawGrid(g, diffX, diffY, w, h, scale);

		for (Obstacle o : obstacles) {
			g.setColor(new Color(0, 0, 0, 64));
			g.drawRect(o.bbx * scale + diffX, o.bby * scale + diffY,
				o.bbwidth * scale, o.bbheight * scale);
		}
		g.drawRect(playerX * scale + diffX, playerY * scale + diffY, 11 * scale, 21 * scale);

		int flags = (int) saveState[0][4];
		StringJoiner flagJoin = new StringJoiner(", ");
		for (int i = 0; i < flagNames.length; i++) {
			if ((flags & (1 << i)) != 0) {
				flagJoin.add(flagNames[i]);
			}
		}
		drawCenteredTextWithBackground(g, Color.black, playerColor, w / 2, h / 2,
			"X: " + Float.toString((float) saveState[0][1]) + "\n" +
			"Y: " + Float.toString((float) saveState[0][2]) + "\n" +
			"V speed:\n" + Float.toString((float) saveState[0][3]) + "\n" +
			"Flags: " + flagJoin.toString() + "\n" + 
			"FAn: " + (int) saveState[0][9] + "f, " + Math.round(saveState[0][10] * 20), 11 * scale);
	}

	boolean dontAccept;

	public synchronized void tick(double[][] saveState, TASClient client) {
		if (dontAccept) {
			if (saveState[0][1] != this.saveState[0][1]) {
				System.out.println("Mismatch x: " + saveState[0][1] + " vs " + this.saveState[0][1]);
				simul.playerX = (float) saveState[0][1];
			}
			if (saveState[0][2] != this.saveState[0][2]) {
				System.out.println("Mismatch y: " + saveState[0][2] + " vs " + this.saveState[0][2]);
				simul.playerY = (float) saveState[0][2];
			}
			if (saveState[0][3] != this.saveState[0][3]) {
				System.out.println("Mismatch vspeed: " + saveState[0][3] + " vs " + this.saveState[0][3]);
				simul.playerSpeed = (float) saveState[0][3];
			}
			try {
				simul.obstacles = client.getObstacles();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			try {
				Inputs in = client.getInputs();
				doInputs(in);
				tickSimulation();
				repaint();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			return;
		}
		try {
			simul.obstacles = client.getObstacles();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		simul.playerX = (float) saveState[0][1];
		simul.playerY = (float) saveState[0][2];
		simul.playerSpeed = (float) saveState[0][3];
		try {
			Inputs in = client.getInputs();
			doInputs(in);
			tickSimulation();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		repaint();
		dontAccept = true;
	}
}
