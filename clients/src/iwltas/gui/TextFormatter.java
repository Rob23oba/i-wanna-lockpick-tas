package iwltas.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.text.*;
import javax.swing.undo.*;
import javax.swing.event.*;

import java.io.*;
import java.nio.charset.*;

import iwltas.*;

public class TextFormatter {
	public static void main(String[] args) throws BadLocationException {
		JFrame frame = new JFrame("TAS Editor");

		JTextPane pane = new JTextPane();
		pane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));

		FormattingListener listen = new FormattingListener(pane.getStyledDocument());
		pane.getDocument().addDocumentListener(listen);

		UndoManager undo = new UndoManager();
		pane.getDocument().addUndoableEditListener(new UndoableEditListener() {
			@Override
			public void undoableEditHappened(UndoableEditEvent ev) {
				if (listen.suppressUpdates) {
					return;
				}
				undo.undoableEditHappened(ev);
			}
		});

		JFileChooser chooser = new JFileChooser(".");
		FileNameExtensionFilter filter = new FileNameExtensionFilter("TAS Files", "txt");
		chooser.setFileFilter(filter);

		File[] openFile = new File[1];

		pane.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent ev) {
				if (ev.isControlDown()) {
					switch (ev.getKeyCode()) {
					case KeyEvent.VK_Z:
						try {
							undo.undo();
						} catch (CannotUndoException ex) {
						}
						break;
					case KeyEvent.VK_Y:
						try {
							undo.redo();
						} catch (CannotRedoException ex) {
						}
						break;
					case KeyEvent.VK_S:
						if (ev.isShiftDown() || openFile[0] != null) {
							int option = chooser.showSaveDialog(frame);
							if (option != JFileChooser.APPROVE_OPTION) {
								break;
							}
							openFile[0] = chooser.getSelectedFile();
							frame.setTitle("TAS Editor - " + openFile[0].getName());
						}
						try (FileWriter fout = new FileWriter(openFile[0], StandardCharsets.UTF_8)) {
							fout.write(pane.getText());
						} catch (Exception ex) {
							JOptionPane.showMessageDialog(frame, "An error occurred while trying to save: " + ex, "Error", JOptionPane.ERROR_MESSAGE);
						}
						break;
					case KeyEvent.VK_O:
						int option = chooser.showOpenDialog(frame);
						if (option != JFileChooser.APPROVE_OPTION) {
							break;
						}
						openFile[0] = chooser.getSelectedFile();
						frame.setTitle("TAS Editor - " + openFile[0].getName());
						try (FileInputStream fin = new FileInputStream(openFile[0])) {
							pane.setText(new String(fin.readAllBytes(), StandardCharsets.UTF_8));
						} catch (Exception ex) {
							JOptionPane.showMessageDialog(frame, "An error occurred while trying to open the file: " + ex, "Error", JOptionPane.ERROR_MESSAGE);
						}
						undo.discardAllEdits();
						break;
					}
				}
			}
		});

		JScrollPane scroll = new JScrollPane(pane);

		frame.add(scroll);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(500, 500);
		frame.setVisible(true);
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

	public static class FormattingListener implements DocumentListener {
		boolean suppressUpdates;
		int compoundUpdatesStart = Integer.MAX_VALUE;
		int compoundUpdatesEnd = 0;

		StyledDocument doc;

		public FormattingListener(StyledDocument doc) {
			this.doc = doc;
		}

		public void changedUpdate(DocumentEvent ev) {
			if (suppressUpdates) {
				return;
			}
			compoundUpdatesStart = Math.min(compoundUpdatesStart, ev.getOffset());
			compoundUpdatesEnd = Math.max(compoundUpdatesEnd, ev.getOffset() + ev.getLength());
			SwingUtilities.invokeLater(() -> {
				if (compoundUpdatesStart > compoundUpdatesEnd) {
					return;
				}
				suppressUpdates = true;
				try {
					doFormatting(doc, compoundUpdatesStart, compoundUpdatesEnd);
				} catch (BadLocationException ex) {
					ex.printStackTrace();
				}
				suppressUpdates = false;
				compoundUpdatesStart = Integer.MAX_VALUE;
				compoundUpdatesEnd = 0;
			});
		}

		public void insertUpdate(DocumentEvent ev) {
			changedUpdate(ev);
		}

		public void removeUpdate(DocumentEvent ev) {
			changedUpdate(ev);
		}
	}

	public static void printStuff(StyledDocument doc) throws BadLocationException {
		String text = doc.getText(0, doc.getLength());
		for (int i = 0; i < text.length(); i++) {
			System.out.print(text.charAt(i) + " " + doc.getCharacterElement(i).getAttributes().getAttribute("state") + " ");
		}
		System.out.println();
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
					doc.setCharacterAttributes(prevAttrStart, i - prevAttrStart, prevAttr, true);
					doc.setCharacterAttributes(prevAttrStart, i - prevAttrStart, states[prevAttrState], false);
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
			doc.setCharacterAttributes(prevAttrStart, i - prevAttrStart, prevAttr, true);
			doc.setCharacterAttributes(prevAttrStart, i - prevAttrStart, states[prevAttrState], false);
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
			if (Character.isWhitespace(ch)) {
				style[0] = base;
				return 0;
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
