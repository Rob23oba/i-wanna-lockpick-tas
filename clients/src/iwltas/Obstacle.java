package iwltas;

/**
 * Used to represent the result of the {@code obstacles} command.
 */
public class Obstacle {
	public int object_index;
	public float x, y, xscale, yscale;
	public int bbx, bby, bbwidth, bbheight;
}
