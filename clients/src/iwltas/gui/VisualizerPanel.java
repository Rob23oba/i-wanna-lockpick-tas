package iwltas.gui;

import java.io.*;
import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import iwltas.*;

@SuppressWarnings("serial")
public class VisualizerPanel extends JPanel {
	public Visualizer v;

	public static Color getColor(int id) {
		int r = id % 7;
		int g = (id / 7) % 7;
		int b = (id / 49) % 6;
		return new Color(255 * r / 6, 255 * g / 6, 255 * b / 5);
	}

	@Override
	public void paintComponent(Graphics g) {
		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());

		g.setColor(Color.red);
		for (Obstacle o : v.obstacles) {
			g.setColor(getColor(o.object_index));
			g.fillRect(o.bbx, o.bby, o.bbwidth, o.bbheight);
			g.setColor(new Color(0, 0, 0, 64));
			g.drawRect(o.bbx, o.bby, o.bbwidth - 1, o.bbheight - 1);
		}
		if (v.location.hasPlayer) {
			g.setColor(Color.blue);

			int px = (int) Math.rint(v.location.x);
			int py = (int) Math.rint(v.location.y);
			g.fillRect(px - 17 + 12, py - 23 + 11, 11, 21);
		}
	}
}
