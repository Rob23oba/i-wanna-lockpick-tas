package iwltas;

/**
 * Used to represent the result of the {@code location} command.
 */
public class LocationInformation {
	public int room;
	public boolean hasPlayer;
	public double x, y, hspeed, vspeed;
}
