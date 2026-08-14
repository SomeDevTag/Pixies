import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * The start screen, painted by hand on a pixel grid so it is made of the same
 * material as the thing it makes. The only real Swing widget is the name field,
 * because hand rolling a text caret is not worth it.
 *
 * Usage:
 *   StartScreen screen = new StartScreen(Panel.defaultPalette());
 *   screen.setOnStart(this::start);
 */
public class StartScreen extends javax.swing.JPanel {

	private static final long serialVersionUID = 1L;

	/** One colour per letter of the wordmark, straight out of the editor palette. */
	private static final Color[] LETTER_COLORS = {
		new Color(255, 69, 0, 255), new Color(255, 168, 0, 255), new Color(255, 214, 53, 255),
		new Color(126, 237, 86, 255), new Color(54, 144, 234, 255), new Color(180, 74, 192, 255)
	};

	// ---------------------------------------------------------------- pixel font
	// A 5x7 bitmap for each letter of the title. The wordmark is literally drawn
	// out of the same pixels the app edits.

	private static final String[][] WORDMARK = {
		{ "11110", "10001", "10001", "11110", "10000", "10000", "10000" }, // P
		{ "11111", "00100", "00100", "00100", "00100", "00100", "11111" }, // I
		{ "10001", "10001", "01010", "00100", "01010", "10001", "10001" }, // X
		{ "11111", "00100", "00100", "00100", "00100", "00100", "11111" }, // I
		{ "11111", "10000", "10000", "11110", "10000", "10000", "11111" }, // E
		{ "01111", "10000", "10000", "01110", "00001", "00001", "11110" }  // S
	};

	private static final int LETTER_SCALE = 6;
	private static final int LETTER_GAP = LETTER_SCALE;

	/** A four pixel sparkle, echoing the ones that turn up in the sample art. */
	private static final String[] SPARKLE = { "010", "111", "010" };

	// ---------------------------------------------------------------- layout

	private static final int W = 440;
	private static final int H = 566;
	private static final int MARGIN = 40;

	private static final int SWATCH = 20;
	private static final int RIBBON_Y = 112;

	private static final Rectangle NEW_TAB = new Rectangle(MARGIN, 168, 170, 42);
	private static final Rectangle OPEN_TAB = new Rectangle(230, 168, 170, 42);
	private static final Rectangle CARD = new Rectangle(MARGIN, 226, 360, 232);
	private static final Rectangle START = new Rectangle(MARGIN, 482, 360, 52);

	// Vertical rhythm inside the card, top to bottom.
	private static final int LABEL_ONE = 256;   // baseline of the first label
	private static final int FIELD_Y = 266;     // the name field, or the file readout
	private static final int FIELD_H = 36;
	private static final int LABEL_TWO = 330;   // baseline of the second label
	private static final int CHIPS_Y = 340;

	private static final Rectangle BROWSE = new Rectangle(CARD.x + 20, 322, 160, 40);

	/** The six canvas sizes, laid out as chips in two rows of three. */
	static final int[] SIZES = { 8, 16, 32, 64, 128, 30 };
	private static final String[] SIZE_LABELS = { "8px", "16px", "32px", "64px", "128px", "30px" };
	private static final int CHIP_W = 106;
	private static final int CHIP_H = 40;
	private static final int CHIP_GAP = 11;

	// ---------------------------------------------------------------- state

	private final Color[] paletteStrip = new Color[16];
	private final JTextField nameField = new JTextField("Untitled");

	private boolean openMode = false;
	private int sizeIndex = 0;
	private String path;
	private int mouseX = -1;
	private int mouseY = -1;

	private transient Runnable onStart = () -> { };

	StartScreen(Color[] editorPalette) {
		System.arraycopy(editorPalette, 0, paletteStrip, 0, paletteStrip.length);

		setLayout(null);
		setPreferredSize(new Dimension(W, H));
		setBackground(Theme.INK);

		nameField.setBounds(CARD.x + 20, FIELD_Y, CARD.width - 40, FIELD_H);
		nameField.setFont(Theme.font(Font.PLAIN, 15));
		nameField.setBackground(Theme.PANEL_DARK);
		nameField.setForeground(Theme.CREAM);
		nameField.setCaretColor(Theme.PEACH);
		nameField.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
		add(nameField);

		MouseAdapter mouse = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				click(e.getX(), e.getY());
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				mouseX = e.getX();
				mouseY = e.getY();
				repaint();
			}

			@Override
			public void mouseExited(MouseEvent e) {
				mouseX = -1;
				mouseY = -1;
				repaint();
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);

		layoutForMode();
	}

	void setOnStart(Runnable listener) {
		onStart = listener;
	}

	// ---------------------------------------------------------------- what the caller needs

	boolean isOpenMode() {
		return openMode;
	}

	int canvasSize() {
		return SIZES[sizeIndex];
	}

	String projectName() {
		return nameField.getText().trim().replace(" ", "");
	}

	String selectedPath() {
		return path;
	}

	// ---------------------------------------------------------------- input

	private void click(int x, int y) {
		if (NEW_TAB.contains(x, y)) {
			openMode = false;
			layoutForMode();
		} else if (OPEN_TAB.contains(x, y)) {
			openMode = true;
			layoutForMode();
		} else if (START.contains(x, y)) {
			onStart.run();
		} else if (openMode && BROWSE.contains(x, y)) {
			browse();
		} else if (!openMode) {
			for (int i = 0; i < SIZES.length; i++) {
				if (chipBounds(i).contains(x, y)) {
					sizeIndex = i;
					break;
				}
			}
		}
		repaint();
	}

	/** The name field only belongs to the new file mode. */
	private void layoutForMode() {
		nameField.setVisible(!openMode);
		repaint();
	}

	private void browse() {
		File start = new File(".");
		if (Saving.exportDir().isDirectory()) {
			start = Saving.exportDir();
		}
		JFileChooser chooser = new JFileChooser(start);
		if (chooser.showOpenDialog(SwingUtilities.getWindowAncestor(this)) == JFileChooser.APPROVE_OPTION) {
			path = chooser.getSelectedFile().getAbsolutePath();
		}
	}

	private static Rectangle chipBounds(int i) {
		int col = i % 3;
		int row = i / 3;
		return new Rectangle(CARD.x + 20 + col * (CHIP_W + CHIP_GAP),
				CHIPS_Y + row * (CHIP_H + CHIP_GAP), CHIP_W, CHIP_H);
	}

	// ---------------------------------------------------------------- painting

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D graphics = (Graphics2D) g;
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		graphics.setColor(Theme.INK);
		graphics.fillRect(0, 0, getWidth(), getHeight());
		paintBackdrop(graphics);
		paintWordmark(graphics);
		paintRibbon(graphics);

		paintTab(graphics, NEW_TAB, "New file", !openMode);
		paintTab(graphics, OPEN_TAB, "Open file", openMode);

		Theme.block(graphics, CARD, Theme.PANEL, Theme.PANEL_DARK);
		if (openMode) {
			paintOpenCard(graphics);
		} else {
			paintNewCard(graphics);
		}

		paintStart(graphics);
	}

	/** A faint pixel grid plus a few sparkles, quiet enough to stay in the back. */
	private void paintBackdrop(Graphics2D graphics) {
		graphics.setColor(Theme.INK_DEEP);
		for (int y = 0; y < getHeight(); y += 8) {
			for (int x = 0; x < getWidth(); x += 8) {
				graphics.fillRect(x, y, 2, 2);
			}
		}
		graphics.setColor(Theme.PEACH);
		Theme.sprite(graphics, SPARKLE, 24, 78, 4);
		Theme.sprite(graphics, SPARKLE, 396, 62, 5);
		Theme.sprite(graphics, SPARKLE, 18, 146, 3);
	}

	/** PIXIES, drawn a pixel at a time, one palette colour per letter. */
	private void paintWordmark(Graphics2D graphics) {
		int letterWidth = 5 * LETTER_SCALE;
		int total = WORDMARK.length * letterWidth + (WORDMARK.length - 1) * LETTER_GAP;
		int x = (W - total) / 2;
		int y = 44;

		for (int i = 0; i < WORDMARK.length; i++) {
			graphics.setColor(Theme.INK_DEEP);
			Theme.sprite(graphics, WORDMARK[i], x + Theme.SHADOW, y + Theme.SHADOW, LETTER_SCALE);
			graphics.setColor(LETTER_COLORS[i]);
			Theme.sprite(graphics, WORDMARK[i], x, y, LETTER_SCALE);
			x += letterWidth + LETTER_GAP;
		}

		graphics.setFont(Theme.font(Font.PLAIN, 13));
		graphics.setColor(Theme.MUTED);
		Theme.centreString(graphics, "pixel art and animation", W / 2, RIBBON_Y + SWATCH + 22);
	}

	/** The editor's 16 swatches, so you know what you are walking into. */
	private void paintRibbon(Graphics2D graphics) {
		int x = (W - 16 * SWATCH) / 2;
		graphics.setColor(Theme.INK_DEEP);
		graphics.fillRect(x + Theme.SHADOW, RIBBON_Y + Theme.SHADOW, 16 * SWATCH, SWATCH);
		for (int i = 0; i < 16; i++) {
			graphics.setColor(paletteStrip[i]);
			graphics.fillRect(x + i * SWATCH, RIBBON_Y, SWATCH, SWATCH);
		}
	}

	private void paintTab(Graphics2D graphics, Rectangle r, String label, boolean active) {
		boolean hot = r.contains(mouseX, mouseY);
		Color face = Theme.PANEL_DARK;
		Color ink = Theme.MUTED;
		if (active) {
			face = Theme.MINT;
			ink = Theme.INK;
		} else if (hot) {
			face = Theme.PANEL;
			ink = Theme.CREAM;
		}
		Theme.block(graphics, r, face, Theme.INK_DEEP);

		graphics.setFont(Theme.font(Font.BOLD, 14));
		graphics.setColor(ink);
		Theme.centreString(graphics, label, r.x + r.width / 2, r.y + r.height / 2 + 5);
	}

	private void paintNewCard(Graphics2D graphics) {
		graphics.setFont(Theme.font(Font.BOLD, 12));
		graphics.setColor(Theme.PEACH);
		graphics.drawString("NAME", CARD.x + 20, LABEL_ONE);
		graphics.drawString("CANVAS SIZE", CARD.x + 20, LABEL_TWO);

		for (int i = 0; i < SIZES.length; i++) {
			Rectangle r = chipBounds(i);
			boolean active = i == sizeIndex;
			boolean hot = r.contains(mouseX, mouseY);
			Color face = Theme.PANEL_DARK;
			Color ink = Theme.CREAM;
			if (active) {
				face = Theme.PEACH;
				ink = Theme.INK;
			} else if (hot) {
				face = Theme.PANEL;
			}
			Theme.block(graphics, r, face, Theme.INK_DEEP);

			graphics.setFont(Theme.font(Font.BOLD, 15));
			graphics.setColor(ink);
			Theme.centreString(graphics, SIZE_LABELS[i], r.x + r.width / 2, r.y + r.height / 2 + 6);
		}
	}

	private void paintOpenCard(Graphics2D graphics) {
		graphics.setFont(Theme.font(Font.BOLD, 12));
		graphics.setColor(Theme.PEACH);
		graphics.drawString("PROJECT FILE", CARD.x + 20, LABEL_ONE);

		Rectangle field = new Rectangle(CARD.x + 20, FIELD_Y, CARD.width - 40, FIELD_H);
		graphics.setColor(Theme.PANEL_DARK);
		graphics.fillRect(field.x, field.y, field.width, field.height);

		graphics.setFont(Theme.font(Font.PLAIN, 13));
		String shown = "No file chosen yet";
		if (path == null) {
			graphics.setColor(Theme.MUTED);
		} else {
			graphics.setColor(Theme.CREAM);
			shown = new File(path).getName();
		}
		graphics.drawString(Theme.clip(graphics, shown, field.width - 20), field.x + 12, field.y + 23);

		boolean hot = BROWSE.contains(mouseX, mouseY);
		Color browseFace = Theme.PANEL_DARK;
		if (hot) {
			browseFace = Theme.PANEL;
		}
		Theme.block(graphics, BROWSE, browseFace, Theme.INK_DEEP);
		graphics.setFont(Theme.font(Font.BOLD, 13));
		graphics.setColor(Theme.CREAM);
		Theme.centreString(graphics, "Choose a file", BROWSE.x + BROWSE.width / 2, BROWSE.y + BROWSE.height / 2 + 5);

		graphics.setFont(Theme.font(Font.PLAIN, 12));
		graphics.setColor(Theme.MUTED);
		graphics.drawString("Only .pixies projects can be opened.", CARD.x + 20, 400);
	}

	private void paintStart(Graphics2D graphics) {
		boolean hot = START.contains(mouseX, mouseY);
		Color face = Theme.FLAME;
		Color ink = Theme.CREAM;
		if (hot) {
			face = Theme.PEACH;
			ink = Theme.INK;
		}
		String label = "Start drawing";
		if (openMode) {
			label = "Open it";
		}
		Theme.block(graphics, START, face, Theme.INK_DEEP);
		graphics.setFont(Theme.font(Font.BOLD, 19));
		graphics.setColor(ink);
		Theme.centreString(graphics, label, START.x + START.width / 2, START.y + START.height / 2 + 7);
	}

	// ---------------------------------------------------------------- helpers

}
