package iwltas.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

import iwltas.*;

public class Analyzer extends JPanel {
	double[][] saveState;
	List<Obstacle> obstacles;
	JFrame frame;

	public Analyzer() {
		setPreferredSize(new Dimension(800, 608));
	}

	public void showFrame() {
		if (frame == null) {
			frame = new JFrame("IWL Analyzer Window");
			frame.add(this);
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent ev) {
					frame = null;
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

	public static void drawGrid(Graphics g, int diffX, int diffY, int w, int h, int scale) {
		int firstX = -Math.floorDiv(diffX, scale); // = ceil(-diffX / scale)
		int firstY = -Math.floorDiv(diffY, scale);
		int lastX = Math.floorDiv(w - diffX, scale);
		int lastY = Math.floorDiv(h - diffY, scale);
		for (int x = firstX; x <= lastX; x++) {
			g.setColor(gridColor(x));
			g.drawLine(x * 8 + diffX, 0, x * 8 + diffX, h);
		}
		for (int y = firstY; y <= lastY; y++) {
			g.setColor(gridColor(y));
			g.drawLine(0, y * 8 + diffY, w, y * 8 + diffY);
		}
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
			g.fillRect((int) o.bbx * scale + diffX, (int) o.bby * scale + diffY,
				(int) o.bbwidth * scale, (int) o.bbheight * scale);
		}

		Color playerColor = VisualizerPanel.getColor(19);
		g.setColor(playerColor); // objPlayer
		g.fillRect(playerX * scale + diffX, playerY * scale + diffY, 11 * scale, 21 * scale);

		drawGrid(g, diffX, diffY, w, h, scale);

		for (Obstacle o : obstacles) {
			g.setColor(new Color(0, 0, 0, 64));
			g.drawRect((int) o.bbx * scale + diffX, (int) o.bby * scale + diffY,
				(int) o.bbwidth * scale, (int) o.bbheight * scale);
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
			"Flags: " + flagJoin.toString(), 11 * scale);
	}

	public synchronized void tick(double[][] saveState, TASClient client) {
		this.saveState = saveState;
		try {
			this.obstacles = client.getObstacles();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		repaint();
	}
}