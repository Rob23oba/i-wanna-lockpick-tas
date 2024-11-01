package iwltas.cli;

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.nio.charset.*;

import iwltas.*;

public class Refactor {
	public static void main(String[] args) {
		File file1 = null;
		File file2 = null;
		String action = null;
		loop: for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "--help":
				showHelp();
				break;
			case "--create":
			case "--inverted-apply":
			case "--apply":
				if (action != null) {
					System.err.println("You can only specify one action");
					showHelp();
				}
				action = args[i];
				break;
			default:
				if (file1 == null) {
					file1 = new File(args[i]);
				} else if (file2 == null) {
					file2 = new File(args[i]);
				} else {
					System.err.println("You can only specify two files");
					showHelp();
				}
			}
		}
		if (args.length == 0) {
			showHelp();
		}
		if ("--create".equals(action)) {
			if (file1 == null) {
				System.err.println("No files specified");
				showHelp();
			}
			if (file2 == null) {
				System.err.println("No target file specified");
				showHelp();
			}
			if (!file1.isDirectory()) {
				System.err.println("The first file doesn't exist or is not a directory");
				showHelp();
			}
			TreeSet<String> tasList = new TreeSet<>();
			try {
				recurseMakeRefactorList(file1, "", tasList);
			} catch (IOException ex) {
				System.err.println("An error occurred while reading TAS files");
				ex.printStackTrace();
				System.exit(1);
			}
			try (PrintStream out = new PrintStream(file2)) {
				for (String s : tasList) {
					out.println(s + " -> " + s);
				}
			} catch (IOException ex) {
				System.err.println("An error occurred while writing a refactoring file");
				ex.printStackTrace();
				System.exit(1);
			}
		} else {
			if (file1 == null) {
				System.err.println("No files specified");
				showHelp();
			}
			if (file2 == null) {
				System.err.println("No refactoring file specified");
				showHelp();
			}
			if (!file1.isDirectory()) {
				System.err.println("The first file doesn't exist or is not a directory");
				showHelp();
			}
			RefactorFile refactor = null;
			try {
				refactor = loadRefactoring(file2, "--inverted-apply".equals(action));
			} catch (IOException ex) {
				System.err.println("An error occurred while reading the refactoring file");
				ex.printStackTrace();
				System.exit(1);
			}
			try {
				refactor(file1, refactor);
			} catch (RuntimeException ex) {
				if (ex.getClass() == RuntimeException.class) {
					System.exit(1);
				}
				throw ex;
			} catch (IOException ex) {
				System.err.println("An error occurred while refactoring");
				ex.printStackTrace();
				System.exit(1);
			}
		}
	}

	public static void showHelp() {
		System.err.println("""
		Usage: java iwltas.cli.Refactor [options] <dir> <refactor_file>
		Options:
		 --help:            Show this help message
		 --create:          Create a refactoring file
		 --inverted-apply:  Apply a refactoring file backwards
		 --apply:           Apply a refactoring file (default)
		""");
		System.exit(1);
	}

	public static void recurseMakeRefactorList(File file, String path, Set<String> into) throws IOException {
		if (file.isDirectory()) {
			for (String s : file.list()) {
				recurseMakeRefactorList(new File(file, s), path.isEmpty() ? s : path + "/" + s, into);
			}
		} else if (path.endsWith(".txt")) {
			String name = path.substring(0, path.length() - 4);
			into.add(name);
			getListOfReferencedTASes(file, into::add);
		}
	}

	public record RefactorFile(Map<String, String> map, Map<String, String> inverse, Map<String, Integer> leftLineNumbers, Map<String, Integer> rightLineNumbers) {
	}

	public static RefactorFile loadRefactoring(File file, boolean invert) throws IOException {
		try (BufferedReader in = new BufferedReader(new FileReader(file))) {
			Map<String, String> map = new HashMap<>();
			Map<String, String> inverse = new HashMap<>();
			Map<String, Integer> prevOccurrenceLeft = new HashMap<>();
			Map<String, Integer> prevOccurrenceRight = new HashMap<>();
			int lineNumber = 0;
			while (true) {
				String line = in.readLine();
				if (line == null) {
					break;
				}
				lineNumber++;
				if (line.startsWith("#")) {
					continue;
				}
				int arrow = line.indexOf("->");
				String from, to;
				if (arrow < 0) {
					int backwardsArrow = line.indexOf("<-");
					if (backwardsArrow < 0) {
						continue;
					}
					from = line.substring(backwardsArrow + 2).strip();
					to = line.substring(0, backwardsArrow).strip();
				} else {
					from = line.substring(0, arrow).strip();
					to = line.substring(arrow + 2).strip();
				}
				Integer old = prevOccurrenceLeft.put(from, lineNumber);
				if (old != null) {
					System.err.println("Duplicate source name '" + from + "': in line " + old + " and " + lineNumber);
					return null;
				}
				old = prevOccurrenceRight.put(to, lineNumber);
				if (old != null) {
					System.err.println("Duplicate target name '" + to + "': in line " + old + " and " + lineNumber);
					return null;
				}
				map.put(from, to);
				inverse.put(to, from);
			}
			if (invert) {
				return new RefactorFile(inverse, map, prevOccurrenceRight, prevOccurrenceLeft);
			} else {
				return new RefactorFile(map, inverse, prevOccurrenceLeft, prevOccurrenceRight);
			}
		}
	}

	public static void refactor(File dir, RefactorFile refactor) throws IOException {
		// Load all TASes into memory
		Map<String, byte[]> allTases = new HashMap<>();
		recurseLoadRefactoredTASes(dir, "", refactor.map(), allTases, (name, source) -> {
			// Safety procaution
			if (refactor.map().containsKey(name)) {
				return;
			}
			String left = refactor.inverse().get(name);
			if (left == null) {
				return;
			}
			System.err.println("Refactored target collides with unrefactored name");
			System.err.println("Refactoring: " + left + " -> " + name + " (in line " + refactor.leftLineNumbers().get(left) + ")");
			if (source != null) {
				System.err.println("Unrefactored name: " + name + " (found in " + source + ")");
			} else {
				System.err.println("Unrefactored name: " + name + " (from file " + name + ".txt)");
			}
			new Exception().printStackTrace();
			// terminate refactoring process
			throw new RuntimeException();
		});
		// Now it *should* be safe to delete and recreate all necessary files
		// However, this does not check for illegal file names
		for (String s : allTases.keySet()) {
			if (!refactor.inverse().containsKey(s)) {
				new File(dir, s + ".txt").delete();
			}
		}
		for (Map.Entry<String, byte[]> entry : allTases.entrySet()) {
			String name = entry.getKey();
			String refactored = refactor.map().get(name);
			if (refactored != null) {
				name = refactored;
			}
			System.out.println("Refactoring " + entry.getKey() + " -> " + name + "...");
			File f = new File(dir, name + ".txt");
			f.getParentFile().mkdirs();
			try (FileOutputStream fout = new FileOutputStream(f)) {
				fout.write(entry.getValue());
			}
		}
	}

	// nameConsumer.accept(name, where)
	public static void recurseLoadRefactoredTASes(File dir, String path, Map<String, String> refactor, Map<String, byte[]> into, BiConsumer<String, String> nameConsumer) throws IOException {
		for (String s : dir.list()) {
			File f = new File(dir, s);
			if (f.isDirectory()) {
				recurseLoadRefactoredTASes(f, path + s + "/", refactor, into, nameConsumer);
			} else if (s.endsWith(".txt")) {
				String name = path + s.substring(0, s.length() - 4);
				nameConsumer.accept(name, null);
				byte[] b = loadRefactoredTAS(f, refactor, x -> nameConsumer.accept(x, name));
				into.put(name, b);
			}
		}
	}

	public static byte[] loadRefactoredTAS(File f, Map<String, String> refactor, Consumer<String> consumer) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ByteArrayOutputStream nameOut = new ByteArrayOutputStream();
		try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(f))) {
			int b = in.read();
			while (b >= 0) {
				if (b == '#') {
					// skip
					do {
						bout.write(b);
						b = in.read();
					} while (b >= 0 && b != '\r' && b != '\n');
					continue;
				}
				if (b == '$') {
					bout.write(b);

					// read
					while (!TASReader.isStopCharacter(b = in.read())) {
						nameOut.write(b);
					}
					String name = nameOut.toString(StandardCharsets.UTF_8);
					consumer.accept(name);
					String refactored = refactor.get(name);
					if (refactored == null) {
						nameOut.writeTo(bout);
					} else {
						bout.writeBytes(refactored.getBytes(StandardCharsets.UTF_8));
					}
					nameOut.reset();
					continue;
				}
				bout.write(b); // just copy anything else
				b = in.read();
			}
		}
		return bout.toByteArray();
	}

	public static void getListOfReferencedTASes(File f, Consumer<String> consumer) throws IOException {
		ByteArrayOutputStream nameOut = new ByteArrayOutputStream();
		try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(f))) {
			int b = in.read();
			while (b >= 0) {
				if (b == '#') {
					// skip
					do {
						b = in.read();
					} while (b >= 0 && b != '\r' && b != '\n');
					continue;
				}
				if (b == '$') {
					// read
					while (!TASReader.isStopCharacter(b = in.read())) {
						nameOut.write(b);
					}
					String name = nameOut.toString(StandardCharsets.UTF_8);
					consumer.accept(name);
					nameOut.reset();
					continue;
				}
				b = in.read();
			}
		}
	}
}
