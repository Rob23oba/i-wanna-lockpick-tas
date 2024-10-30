package iwltas.gui;

import java.awt.*;
import java.awt.image.*;
import java.io.*;

public class Masks {
	public static class Mask {
		public final int width;
		public final int height;
		public final int originX;
		public final int originY;
		byte[] data;

		public Mask(int width, int height, int ox, int oy) {
			this.width = width;
			this.height = height;
			this.originX = ox;
			this.originY = oy;
			this.data = new byte[(width + 7) / 8 * height];
		}

		public BufferedImage toImage(Color fg) {
			IndexColorModel model = new IndexColorModel(1, 2,
				new byte[] { 0, (byte) fg.getRed() },
				new byte[] { 0, (byte) fg.getGreen() },
				new byte[] { 0, (byte) fg.getBlue() },
				new byte[] { 0, (byte) fg.getAlpha() }
			);
			BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY, model);
			int strip = (width + 7) / 8;
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					int pos = i * strip + j / 8;
					int bit = 1 << ((j & 7) ^ 7);
					if ((data[pos] & bit) != 0) {
						img.setRGB(j, i, fg.getRGB());
					} else {
						img.setRGB(j, i, 0);
					}
				}
			}
			return img;
		}
	}

	public static final Mask[] masks = load();

	private static Mask[] load() {
		try (InputStream in = Masks.class.getResourceAsStream("masks.bin")) {
			DataInputStream din = new DataInputStream(in);
			int count = din.readInt();
			Mask[] array = new Mask[count];
			for (int i = 0; i < count; i++) {
				Mask m = new Mask(din.readInt(), din.readInt(), din.readInt(), din.readInt());
				din.readFully(m.data);
				array[i] = m;
			}
			return array;
		} catch (Exception ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}
}
