package iwltas.gui;

import java.io.*;
import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import iwltas.*;

public class VisualizerPanel extends JPanel {
	public Visualizer v;

	@Override
	public void paintComponent(Graphics g) {
		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());

		g.setColor(Color.red);
		for (Obstacle o : v.obstacles) {
			g.fillRect((int) o.bbx, (int) o.bby, (int) o.bbwidth, (int) o.bbheight);
		}
		if (v.location.hasPlayer) {
			g.setColor(Color.blue);

			int px = (int) Math.rint(v.location.x);
			int py = (int) Math.rint(v.location.y);
			g.fillRect(px - 17 + 12, py - 23 + 11, 11, 21);
		}
	}
}
