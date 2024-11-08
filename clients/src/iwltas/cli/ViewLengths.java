package iwltas.cli;

import java.io.*;
import java.util.*;

import iwltas.*;

public class ViewLengths {
	public static void main(String[] args) {
		boolean help = false;
		boolean hide = false;
		int depth = 1;
		File dir = null;
		File file = null;
		File compareDir = null;
		String expr = null;
		boolean selfLength = false;
		loop: for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "--help":
				help = true;
				break loop;
			case "--hide-incomplete":
				hide = true;
				break;
			case "--lookup":
				if (i + 1 >= args.length) {
					System.err.println("--lookup requires a directory argument");
					help = true;
					break loop;
				}
				dir = new File(args[++i]);
				break;
			case "-r":
			case "--recursive":
				depth = -1;
				break;
			case "--compare":
				if (i + 1 >= args.length) {
					System.err.println("--compare requires a directory argument");
					help = true;
					break loop;
				}
				compareDir = new File(args[++i]);
				break;
			case "-e":
				if (file != null) {
					System.err.println("-e cannot be used when a file is provided");
					help = true;
					break loop;
				}
				if (i + 1 >= args.length) {
					System.err.println("--lookup requires a TAS argument");
					help = true;
					break loop;
				}
				expr = args[++i];
				break;
			case "--self-length":
				selfLength = true;
				break;
			default:
				if (file != null) {
					System.err.println("You can only specify one file");
					help = true;
					break loop;
				}
				if (expr != null) {
					System.err.println("-e cannot be used when a file is provided");
					help = true;
					break loop;
				}
				file = new File(args[i]);
			}
		}
		if (args.length == 0) {
			help = true;
		} else if (file == null && expr == null) {
			System.err.println("No file selected");
			help = true;
		}
		if (help) {
			System.err.println("""
			Usage: java iwltas.cli.ViewLengths [options] <dir/file>
			Options:
			 --help:            Show this help message
			 --hide-incomplete: Hide lengths of incomplete TASes (TASes that reference non-existent TASes)
			 --lookup <dir>:    Use a different lookup directory
			 --compare <dir>:   Compare results with a different lookup
			 --self-length:     Only show the lengths of the TASes themselves without their references
			 -e "...":          Show length of provided TAS
			 -r, --recursive:   Traverse the entire file tree from the provided directory
			""");
			System.exit(1);
		}
		if (dir == null && file != null) {
			dir = file;
		} else if (dir == null) {
			dir = new File(".");
		}
		if (!dir.isDirectory()) {
			dir = new File(dir, "..");
		}
		TASLookup lookup = TASLookup.fromLookupDirectory(dir);
		TASLookup compareLookup = compareDir != null ? TASLookup.fromLookupDirectory(compareDir) : null;
		if (selfLength) {
			lookup = name -> { throw new IOException(); };
			compareLookup = name -> { throw new IOException(); };
		}

		List<Result> results = new ArrayList<>();
		if (file != null) {
			recurseCalculateLengths(file, "", depth, lookup, compareDir, compareLookup, results);
		}
		if (expr != null) {
			try (StringReader in = new StringReader(expr)) {
				results.add(readerLength(expr, lookup, in));
			} catch (IOException ex) {
				results.add(new Result(expr, "error", 0));
			}
		}
		int longestName = 0;
		for (Result r : results) {
			if (hide && r.report().endsWith("(incomplete)")) {
				continue;
			}
			longestName = Math.max(longestName, r.fileName().length());
		}
		long prevCompare = 0L;
		long diffTotal = 0L;
		for (Result r : results) {
			if (hide && r.report().endsWith("(incomplete)")) {
				continue;
			}
			String suffix = "";
			if (compareDir != null && r.fileName().startsWith("a/")) {
				prevCompare = r.length();
			} else if (compareDir != null && r.fileName().startsWith("b/")) {
				suffix = " (" + lengthDiff(r.length() - prevCompare) + ")";
				diffTotal += r.length() - prevCompare;
			}
			System.out.println(r.fileName() + " ".repeat(longestName + 4 - r.fileName().length()) + r.report() + suffix);
		}
		if (compareDir != null) {
			System.out.println("Total: " + lengthDiff(diffTotal));
		}
	}

	public static String lengthDiff(long diff) {
		if (diff < 0) {
			return diff + " frames";
		} else if (diff > 0) {
			return "+" + diff + " frames";
		} else {
			return "unchanged";
		}
	}

	public record Result(String fileName, String report, long length) {
	}

	public static void recurseCalculateLengths(File file, String path, int depth, TASLookup lookup, File compareFile, TASLookup compareLookup, List<Result> out) {
		if (file.isDirectory()) {
			if (depth == 0) {
				return;
			}
			if (compareFile != null && !compareFile.isDirectory()) {
				return;
			}
			for (String s : file.list()) {
				File f = null;
				if (compareFile != null) {
					f = new File(compareFile, s);
					if (!f.exists()) {
						continue;
					}
				}
				recurseCalculateLengths(new File(file, s), path.isEmpty() ? s : path + "/" + s, depth < 0 ? depth : depth - 1, lookup, f, compareLookup, out);
			}
		} else if (file.getName().endsWith(".txt")) {
			if (compareFile != null && !compareFile.isFile()) {
				return;
			}
			String compPath = compareFile == null ? path : "a/" + path;
			try (FileReader in = new FileReader(file)) {
				out.add(readerLength(compPath, lookup, in));
			} catch (IOException ex) {
				out.add(new Result(compPath, "error", 0));
			}
			if (compareFile != null) {
				try (FileReader in = new FileReader(compareFile)) {
					out.add(readerLength("b/" + path, compareLookup, in));
				} catch (IOException ex) {
					out.add(new Result("b/" + path, "error", 0));
				}
			}
		}
	}

	public static void recurseCalculateLengths(File file, String path, int depth, TASLookup lookup, List<Result> out) {
		if (file.isDirectory()) {
			if (depth == 0) {
				return;
			}
			for (String s : file.list()) {
				recurseCalculateLengths(new File(file, s), path.isEmpty() ? s : path + "/" + s, depth < 0 ? depth : depth - 1, lookup, out);
			}
		} else if (file.getName().endsWith(".txt")) {
			try (FileReader in = new FileReader(file)) {
				out.add(readerLength(path, lookup, in));
			} catch (IOException ex) {
				out.add(new Result(path, "error", 0));
			}
		}
	}

	public static Result readerLength(String path, TASLookup lookup, Reader in) throws IOException {
		TASReader reader = new TASReader(in);
		boolean[] incomplete = new boolean[1];
		long length = reader.getLength(lookup.onFailure(x -> incomplete[0] = true));
		String result = length + " frames = " + formatFrames(length);
		if (incomplete[0]) {
			result += " (incomplete)";
		}
		return new Result(path, result, length);
	}

	public static String twoDecimals(long number) {
		return new String(new char[] {
			(char) (number / 10 + '0'),
			(char) (number % 10 + '0')
		});
	}

	public static String formatFrames(long frames) {
		long seconds = frames / 50;
		long minutes = seconds / 60;
		long hours = minutes / 60;
		frames %= 50;
		seconds %= 60;
		minutes %= 60;
		if (hours > 0) {
			return hours + ":" + twoDecimals(minutes) + ":" + twoDecimals(seconds) + "." + twoDecimals(frames * 2);
		}
		return minutes + ":" + twoDecimals(seconds) + "." + twoDecimals(frames * 2);
	}
}
