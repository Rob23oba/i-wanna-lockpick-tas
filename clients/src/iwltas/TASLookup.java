package iwltas;

import java.io.*;
import java.util.function.*;

/**
 * An interface for looking up TAS files.
 */
public interface TASLookup {
	Reader open(String tasName) throws IOException;

	public static TASLookup fromLookupDirectory(File dir) {
		return name -> {
			try (FileInputStream fin = new FileInputStream(new File(dir, name + ".txt"))) {
				return new StringReader(new String(fin.readAllBytes()).stripTrailing());
			}
		};
	}

	default TASLookup onFailure(Consumer<String> failure) {
		return name -> {
			try {
				return open(name);
			} catch (IOException ex) {
				failure.accept(name);
				throw ex;
			}
		};
	}
}
