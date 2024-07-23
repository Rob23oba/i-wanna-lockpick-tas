package iwltas;

import java.io.*;

/**
 * Represents a frame handler which runs every frame.
 * Used for {@link TASClient#runBlocking}.
 *
 * @see TASClient#runBlocking
 */
@FunctionalInterface
public interface TASFrameHandler {
	/**
         * Processes a single frame with the given client.
         *
         * @param client the client to connect 
         */
	void frame(TASClient client) throws IOException;
}
