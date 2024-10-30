package iwltas.cli;

import java.io.*;
import java.util.*;

import iwltas.*;

public class ViewIncomplete {
	public static void main(String[] args) {
		boolean help = false;
		File dir = null;
		File file = null;
		boolean delStub = false;
		boolean stub = false;
		loop: for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "--help":
				help = true;
				break loop;
			case "--lookup":
				if (i + 1 >= args.length) {
					System.err.println("--lookup requires a directory argument");
					help = true;
					break loop;
				}
				dir = new File(args[++i]);
				break;
			case "--delete-stubs":
				if (stub) {
					System.err.println("Cannot --delete-stubs and --create-stubs at the same time");
					help = true;
					break loop;
				}
				delStub = true;
				break;
			case "--create-stubs":
				if (delStub) {
					System.err.println("Cannot --delete-stubs and --create-stubs at the same time");
					help = true;
					break loop;
				}
				stub = true;
				break;
			default:
				if (file != null) {
					System.err.println("You can only specify one file");
					help = true;
					break loop;
				}
				file = new File(args[i]);
			}
		}
		if (args.length == 0) {
			help = true;
		} else if (file == null) {
			System.err.println("No file selected");
			help = true;
		}
		if (help) {
			System.err.println("""
			Usage: java iwltas.cli.ViewIncomplete [options] <file>
			Options:
			 --help:         Show this help message
			 --lookup <dir>: Use the provided lookup directory
			 --delete-stubs: Delete all stubs referenced by the file
			 --create-stubs: Create empty TASes marked "#tmp_stub"
			""");
			System.exit(1);
		}
		if (dir == null) {
			dir = new File(file, "..");
		}
		TASLookup rawLookup = TASLookup.fromLookupDirectory(dir);
		List<String> incompletes = new ArrayList<>();

		// Why, Java, why?!!
		final boolean delStub2 = delStub;
		final boolean stub2 = stub;
		final File dir2 = dir;

		TASLookup lookup = name -> {
			try {
				BufferedReader in = new BufferedReader(rawLookup.open(name));
				boolean doDelete = false;
				in.mark(100);
				String line = in.readLine();
				if ("#tmp_stub".equals(line)) {
					in.reset();
					if (delStub2) {
						in.close();
						doDelete = true;
					} else {
						incompletes.add(name);
					}
				}
				if (!doDelete) {
					return in;
				}
				incompletes.add(name);
			} catch (IOException ex) {
				incompletes.add(name);
				if (stub2) {
					try (FileOutputStream out = new FileOutputStream(new File(dir2, name + ".txt"))) {
						out.write("#tmp_stub\n$exit".getBytes());
					}
					return new StringReader("");
				}
				throw ex;
			}
			new File(dir2, name + ".txt").delete();
			throw new IOException("Delete stub");
		};
		try (FileReader in = new FileReader(file)) {
			TASReader reader = new TASReader(in);
			while (true) {
				long mask = reader.frame(0, 0, lookup);
				if ((mask & TASReader.END_MASK) != 0) {
					break;
				}
				reader.waitTime = 0;
				reader.waitDecimal = 0;
			}
		} catch (Exception ex) {
			System.err.println("An error occurred:");
			ex.printStackTrace();
		}
		System.out.println("Referenced TASes:");
		StringJoiner join = new StringJoiner(", ");
		for (String s : incompletes) {
			join.add(s);
		}
		System.out.println(join);
	}
}
