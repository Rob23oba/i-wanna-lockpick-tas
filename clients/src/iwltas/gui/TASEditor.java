package iwltas.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.text.*;
import javax.swing.event.*;

import java.util.*;
import java.io.*;
import java.nio.charset.*;

import iwltas.*;

public class TASEditor {
	public static class CoolLayout implements LayoutManager {
		public void addLayoutComponent(String name, Component comp) {
		}

		public void removeLayoutComponent(Component comp) {
		}

		public Dimension minimumLayoutSize(Container parent) {
			Dimension top = parent.getComponent(0).getMinimumSize();
			Dimension bottom = parent.getComponent(1).getMinimumSize();
			return new Dimension(Math.max(top.width, bottom.width), top.height + bottom.height);
		}

		public Dimension preferredLayoutSize(Container parent) {
			Dimension top = parent.getComponent(0).getPreferredSize();
			Dimension bottom = parent.getComponent(1).getPreferredSize();
			return new Dimension(Math.max(top.width, bottom.width), top.height + bottom.height);
		}

		public void layoutContainer(Container parent) {
			Dimension outerSize = parent.getSize();

			Component top = parent.getComponent(0);
			Component bottom = parent.getComponent(1);
			Dimension bottomMin = bottom.getMinimumSize();
			top.setBounds(0, 0, outerSize.width, outerSize.height - bottomMin.height);
			bottom.setBounds(0, outerSize.height - bottomMin.height, outerSize.width, bottomMin.height);
		}
	}

	public static void main(String[] args) throws BadLocationException {
		JFrame frame = new JFrame("TAS Editor");

		TASEditor editor = new TASEditor();

		JFileChooser chooser = new JFileChooser(".");
		FileNameExtensionFilter filter = new FileNameExtensionFilter("TAS Files", "txt");
		chooser.setFileFilter(filter);

		editor.pane.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent ev) {
				if (ev.isControlDown()) {
					switch (ev.getKeyCode()) {
					case KeyEvent.VK_S:
						if (ev.isShiftDown() || editor.openFile == null) {
							int option = chooser.showSaveDialog(frame);
							if (option != JFileChooser.APPROVE_OPTION) {
								break;
							}
							editor.openFile = chooser.getSelectedFile();
							frame.setTitle("TAS Editor - " + editor.openFile.getName());
							editor.openedTASName = editor.openFile.getName();
						}
						try (FileWriter fout = new FileWriter(editor.openFile, StandardCharsets.UTF_8)) {
							fout.write(editor.pane.getText());
						} catch (Exception ex) {
							JOptionPane.showMessageDialog(frame, "An error occurred while trying to save: " + ex, "Error", JOptionPane.ERROR_MESSAGE);
						}
						break;
					case KeyEvent.VK_O:
						int option = chooser.showOpenDialog(frame);
						if (option != JFileChooser.APPROVE_OPTION) {
							break;
						}
						editor.openFile = chooser.getSelectedFile();
						frame.setTitle("TAS Editor - " + editor.openFile.getName());
						editor.openedTASName = editor.openFile.getName();
						try (FileInputStream fin = new FileInputStream(editor.openFile)) {
							editor.pane.setText(new String(fin.readAllBytes(), StandardCharsets.UTF_8));
						} catch (Exception ex) {
							JOptionPane.showMessageDialog(frame, "An error occurred while trying to open the file: " + ex, "Error", JOptionPane.ERROR_MESSAGE);
						}
						editor.undo.discardAllEdits();
						break;
					}
				}
			}
		});

		frame.setLayout(new CoolLayout());
		frame.add(editor.scroll);
		frame.add(editor.startingPoint);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(500, 500);
		frame.setVisible(true);

		try (TASClient client = new TASClient()) {
			client.runBlocking(editor.cache);
		} catch (Exception ex) {
			System.err.println("Couldn't connect to remote: " + ex);
		}
	}

	public JTextPane pane;
	public FormattingListener listen;
	public UndoFilter undo;
	public JScrollPane scroll;

	public SaveStateCache cache;

	public JTextField startingPoint;

	public String openedTASName = "";
	public File openFile;

	public TASLookup lookup = name -> {
		if (openFile == null) {
			throw new IOException("No file opened");
		}
		File f = new File(openFile.getParentFile(), name + ".txt");
		try (FileInputStream fin = new FileInputStream(f)) {
			return new StringReader(new String(fin.readAllBytes()).stripTrailing());
		}
	};

	public Analyzer analyzer;

	public TASEditor() {
		pane = new JTextPane() {
			public boolean getScrollableTracksViewportWidth() {
				return false;
			}

			public void setSize(Dimension d) {
				if (d.width < getParent().getSize().width) {
					d.width = getParent().getSize().width;
				}
				super.setSize(d);
			}
		};
		Insets i = pane.getInsets();
		pane.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent ev) {
				SwingUtilities.invokeLater(() -> {
					try {
						JTextComponent c = (JTextComponent) ev.getSource();
						int pos = c.getCaretPosition();
						Rectangle2D r = c.modelToView2D(pos);
						c.scrollRectToVisible(new Rectangle2D.Double(
							r.getX() - i.left,
							r.getY() - i.top,
							r.getWidth() + i.left + i.right,
							r.getHeight() + i.top + i.bottom
						).getBounds());
					} catch (BadLocationException ex) {
					}
				});
			}
		});
		pane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));

		listen = new FormattingListener(pane.getStyledDocument());
		pane.getDocument().addDocumentListener(listen);

		undo = new UndoFilter();
		((AbstractDocument) pane.getDocument()).setDocumentFilter(undo);

		pane.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent ev) {
				keyAction(ev);
			}
		});

		cache = new SaveStateCache();
		startingPoint = new JTextField();
		startingPoint.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent ev) {
				queuedUpdateCacheTarget = true;
				SwingUtilities.invokeLater(TASEditor.this::updateCacheTarget);
			}

			public void insertUpdate(DocumentEvent ev) {
				changedUpdate(ev);
			}

			public void removeUpdate(DocumentEvent ev) {
				changedUpdate(ev);
			}
		});

		pane.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent ev) {
				queuedUpdateCacheTarget = true;
				SwingUtilities.invokeLater(TASEditor.this::updateCacheTarget);
			}
		});

		scroll = new JScrollPane(pane);

		analyzer = new Analyzer();
	}

	public static class UndoFilter extends DocumentFilter {
		record Undo(int off, String prevText, String newText) {
		}

		Vector<Undo> undos = new Vector<>();
		int undoPos = 0;
		boolean suppressUpdates;

		public static Undo mergeUndos(Undo first, Undo second) {
			return null; // don't merge undos
		}

		public void discardAllEdits() {
			undos.clear();
			undoPos = 0;
		}

		public void undo(Document doc) {
			if (undoPos <= 0) {
				return;
			}
			suppressUpdates = true;

			try {
				Undo undo = undos.get(--undoPos);
				if (!undo.newText().isEmpty()) {
					doc.remove(undo.off(), undo.newText().length());
				}
				if (!undo.prevText().isEmpty()) {
					doc.insertString(undo.off(), undo.prevText(), null);
				}
			} catch (BadLocationException ex) {
				ex.printStackTrace();
			} finally {
				suppressUpdates = false;
			}
		}

		public void redo(Document doc) {
			if (undoPos >= undos.size()) {
				return;
			}
			suppressUpdates = true;

			try {
				Undo undo = undos.get(undoPos++);
				if (!undo.prevText().isEmpty()) {
					doc.remove(undo.off(), undo.prevText().length());
				}
				if (!undo.newText().isEmpty()) {
					doc.insertString(undo.off(), undo.newText(), null);
				}
			} catch (BadLocationException ex) {
				ex.printStackTrace();
			} finally {
				suppressUpdates = false;
			}
		}

		public void addUndo(int off, String prevText, String newText) {
			Undo newUndo = new Undo(off, prevText, newText);
			if (undos.size() > undoPos) {
				undos.setSize(undoPos);
			}
			if (undoPos > 0) {
				Undo merge = mergeUndos(undos.get(undoPos - 1), newUndo);
				if (merge != null) {
					undos.set(undoPos - 1, merge);
					return;
				}
			}
			undos.add(newUndo);
			undoPos++;
		}

		public void insertString(DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
			if (!suppressUpdates) {
				addUndo(offset, "", string);
			}
			fb.insertString(offset, string, attr);
		}

		public void remove(DocumentFilter.FilterBypass fb, int offset, int length) throws BadLocationException {
			if (!suppressUpdates) {
				String text;
				try {
					text = fb.getDocument().getText(offset, length);
				} catch (BadLocationException ex) {
					text = "";
					ex.printStackTrace();
				}
				addUndo(offset, text, "");
			}
			fb.remove(offset, length);
		}

		public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String newText, AttributeSet attr) throws BadLocationException {
			if (!suppressUpdates) {
				String text;
				try {
					text = fb.getDocument().getText(offset, length);
				} catch (BadLocationException ex) {
					text = "";
					ex.printStackTrace();
				}
				addUndo(offset, text, newText);
			}
			fb.replace(offset, length, newText, attr);
		}
	}

	public void keyAction(KeyEvent ev) {
		if (!ev.isControlDown()) {
			return;
		}
		switch (ev.getKeyCode()) {
		case KeyEvent.VK_Z:
			undo.undo(pane.getDocument());
			break;
		case KeyEvent.VK_Y:
			undo.redo(pane.getDocument());
			break;
		case KeyEvent.VK_Q:
			try {
				Caret c = pane.getCaret();
				int start = c.getDot();
				int end = c.getMark();
				boolean inv = false;
				if (start > end) {
					int temp = start;
					start = end;
					end = temp;

					inv = true;
				}
				Document doc = pane.getDocument();
				String str = doc.getText(start, end - start);
				String newStr = TASReader.convertToBasicFormat(str, lookup);
				if (!newStr.equals(str)) {
					doc.remove(start, end - start);
					doc.insertString(start, newStr, null);
					if (inv) {
						c.setDot(start);
						c.moveDot(start + newStr.length());
					} else {
						c.setDot(start + newStr.length());
						c.moveDot(start);
					}
				}
			} catch (BadLocationException | IOException ex) {
			}
			break;
		case KeyEvent.VK_L:
			try {
				Caret c = pane.getCaret();
				int start = c.getDot();
				int end = c.getMark();
				boolean inv = false;
				if (start > end) {
					int temp = start;
					start = end;
					end = temp;

					inv = true;
				}
				Document doc = pane.getDocument();
				String str = doc.getText(start, end - start);
				TASReader reader = new TASReader(new StringReader(str));
				long length = reader.getLength(lookup);
				String suffix = ") " + length;
				doc.insertString(end, suffix, null);
				doc.insertString(start, "(", null);
				if (inv) {
					c.setDot(start);
					c.moveDot(end + suffix.length() + 1);
				} else {
					c.setDot(end + suffix.length() + 1);
					c.moveDot(start);
				}
			} catch (BadLocationException | IOException ex) {
			}
			break;
		case KeyEvent.VK_T:
			analyzer.showFrame();
			analyzer.frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent ev) {
					cache.analyzer = null;
				}
			});
			cache.analyzer = analyzer::tick;
			break;
		}
	}

	private class SpecialBreakpointLookup implements TASLookup {
		boolean end;

		@Override
		public Reader open(String name) throws IOException {
			if (!(name + ".txt").equals(openedTASName)) {
				return lookup.open(name);
			}
			Document doc = pane.getDocument();
			String allText = "";
			try {
				allText = doc.getText(0, doc.getLength());
			} catch (BadLocationException ex) {}
			return new StringReaderWithBreakpoint(allText, pane.getCaret().getDot(), () -> end = true);
		}
	}

	boolean queuedUpdateCacheTarget;
	String oldStartingPoint = "";

	public void updateCacheTarget() {
		if (!queuedUpdateCacheTarget) {
			return;
		}
		queuedUpdateCacheTarget = false;

		InputSequence seq = new InputSequence();
		String text = startingPoint.getText();

		TASReader reader = new TASReader(new StringReader(text));
		int prevInputs = 0;

		SpecialBreakpointLookup newLookup = new SpecialBreakpointLookup();
		try {
			while (true) {
				long inputMask = reader.frame(prevInputs, newLookup);
				if (newLookup.end) {
					break;
				} else if ((inputMask & TASReader.END_MASK) != 0) {
					seq = null;
					break;
				}
				seq.add(inputMask);
				prevInputs = TASReader.applyInputMask(inputMask, prevInputs);
			}
		} catch (IOException ex) {
			seq = null;
		}
		synchronized (cache) {
			if (!text.equals(oldStartingPoint)) {
				cache.clear();
				oldStartingPoint = text;
			}
			cache.target = seq;
			cache.interrupt();
		}
	}

	public static class FormattingListener implements DocumentListener {
		boolean suppressUpdates;
		int compoundUpdatesStart = Integer.MAX_VALUE;
		int compoundUpdatesEnd = -1;

		StyledDocument doc;

		public FormattingListener(StyledDocument doc) {
			this.doc = doc;
		}

		public void changedUpdate(DocumentEvent ev) {
			//System.out.println("Change " + ev.getOffset() + " " + ev.getLength());
		}

		public void formatUpdate() {
			if (compoundUpdatesEnd < 0) {
				return;
			}
			//compoundUpdatesStart = Math.max(0, compoundUpdatesStart);
			//compoundUpdatesEnd = Math.min(doc.getLength(), compoundUpdatesEnd);
			suppressUpdates = true;
			try {
				doFormatting(doc, compoundUpdatesStart, compoundUpdatesEnd);
			} catch (BadLocationException ex) {
				ex.printStackTrace();
			}
			suppressUpdates = false;
			compoundUpdatesStart = Integer.MAX_VALUE;
			compoundUpdatesEnd = -1;
			System.out.println("Formatting done");
		}

		public void insertUpdate(DocumentEvent ev) {
			System.out.println("Insertion " + ev.getOffset() + " " + ev.getLength());
			if (compoundUpdatesEnd < 0) {
				SwingUtilities.invokeLater(this::formatUpdate);
				compoundUpdatesStart = ev.getOffset();
				compoundUpdatesEnd = ev.getOffset() + ev.getLength();
				return;
			}
			compoundUpdatesStart = Math.min(compoundUpdatesStart, ev.getOffset());
			if (ev.getOffset() <= compoundUpdatesEnd) {
				compoundUpdatesEnd += ev.getLength();
			} else {
				compoundUpdatesEnd = ev.getOffset() + ev.getLength();
			}
		}

		public void removeUpdate(DocumentEvent ev) {
			System.out.println("Deletion " + ev.getOffset() + " " + ev.getLength());
			try {
				Document doc = ev.getDocument();
				String text = doc.getText(0, doc.getLength());
				System.out.println(text);
			} catch (BadLocationException ex) {
				ex.printStackTrace();
			}
			if (compoundUpdatesEnd < 0) {
				SwingUtilities.invokeLater(this::formatUpdate);
				compoundUpdatesStart = ev.getOffset();
				compoundUpdatesEnd = compoundUpdatesStart;
				return;
			}
			compoundUpdatesStart = Math.min(compoundUpdatesStart, ev.getOffset());
			int deletionEnd = ev.getOffset() + ev.getLength();
			if (deletionEnd <= compoundUpdatesEnd) {
				compoundUpdatesEnd -= ev.getLength();
			} else {
				compoundUpdatesEnd = ev.getOffset();
			}
		}
	}

	public static void printStuff(StyledDocument doc) throws BadLocationException {
		String text = doc.getText(0, doc.getLength());
		for (int i = 0; i < text.length(); i++) {
			System.out.print(text.charAt(i) + " " + doc.getCharacterElement(i).getAttributes().getAttribute("state") + " ");
		}
		System.out.println();
	}

	public static final MutableAttributeSet base = new SimpleAttributeSet();
	public static final MutableAttributeSet actions = new SimpleAttributeSet();
	public static final MutableAttributeSet wait = new SimpleAttributeSet();
	public static final MutableAttributeSet call = new SimpleAttributeSet();
	public static final MutableAttributeSet comment = new SimpleAttributeSet();

	public static final MutableAttributeSet state0 = new SimpleAttributeSet();
	public static final MutableAttributeSet state1 = new SimpleAttributeSet();
	public static final MutableAttributeSet state2 = new SimpleAttributeSet();

	static {
		StyleConstants.setForeground(actions, new Color(0xFF0000));
		StyleConstants.setForeground(wait, new Color(0x0000FF));
		StyleConstants.setForeground(call, new Color(0xC08000));
		StyleConstants.setForeground(comment, new Color(0x408000));

		state0.addAttribute("state", 0);
		state1.addAttribute("state", 1);
		state2.addAttribute("state", 2);
	}

	/**
	 * Reformats the document. The parameters from and to specify the range of the change.
	 *
	 * @param doc the document to reformat.
	 * @param start where to start the reformatting.
	 * @param end where the change ended. However, reformatting might go beyond that index.
	 */
	public static void doFormatting(StyledDocument doc, int start, int end) throws BadLocationException {
		AttributeSet[] states = { state0, state1, state2 };

		int length = doc.getLength();

		int curState = 0;
		if (start > 0) {
			curState = (int) doc.getCharacterElement(start - 1).getAttributes().getAttribute("state");
		}

		AttributeSet prevAttr = null;
		int prevAttrState = -1;
		int prevAttrStart = 0;

		AttributeSet[] attrHolder = new AttributeSet[1];

		int textEnd = Math.min(length, end + 1);

		String text = doc.getText(start, textEnd - start);
		int i = start;
		while (i < textEnd) {
			curState = transitionSingle(attrHolder, curState, text.charAt(i - start));
			if (attrHolder[0] != prevAttr || prevAttrState != curState) {
				if (prevAttr != null) {
					SimpleAttributeSet newAttr = new SimpleAttributeSet(prevAttr);
					newAttr.addAttribute("state", prevAttrState);
					doc.setCharacterAttributes(prevAttrStart, i - prevAttrStart, newAttr, true);
				}
				prevAttrStart = i;
				prevAttr = attrHolder[0];
				prevAttrState = curState;
			}
			i++;
			if (i >= end) {
				if (i >= length) {
					break;
				}
				int state = (int) doc.getCharacterElement(i).getAttributes().getAttribute("state");
				if (state == curState) {
					break;
				}
				if (i >= textEnd) {
					textEnd = Math.min(length, i + 100);
					text = doc.getText(i, textEnd - i);
					start = i;
				}
			}
		}
		if (prevAttr != null) {
			SimpleAttributeSet newAttr = new SimpleAttributeSet(prevAttr);
			newAttr.addAttribute("state", prevAttrState);
			doc.setCharacterAttributes(prevAttrStart, i - prevAttrStart, newAttr, true);
		}
	}

	private static int transitionSingle(AttributeSet[] style, int state, char ch) {
		switch (state) {
		case 0:
			if (Inputs.TAS_STRING.indexOf(ch) >= 0) {
				style[0] = actions;
				return 0;
			} else if (Inputs.TAS_STRING_OFF.indexOf(ch) >= 0) {
				style[0] = actions;
				return 0;
			} else if (ch >= '0' && ch <= '9') {
				style[0] = wait;
				return 0;
			} else if (ch == '$') {
				style[0] = call;
				return 1;
			} else if (ch == '#') {
				style[0] = comment;
				return 2;
			}
			break;
		case 1:
			if (TASReader.isStopCharacter(ch)) {
				return transitionSingle(style, 0, ch);
			} else {
				style[0] = call;
				return 1;
			}
		case 2:
			if (ch == '\n') {
				style[0] = base;
				return 0;
			} else {
				style[0] = comment;
				return 2;
			}
		}
		style[0] = base;
		return 0;
	}
}
