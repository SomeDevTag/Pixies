import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

/**
 * The shortcut list, painted by hand so it matches the rest of the app.
 * Keys are drawn as little caps instead of being spelled out in a text blob.
 *
 * Usage:
 *   HelpScreen screen = new HelpScreen();
 *   screen.setOnClose(frame::dispose);
 */
public class HelpScreen extends JPanel {

	private static final long serialVersionUID = 1L;

	// ---------------------------------------------------------------- content

	/** One row: the keys you press, and what pressing them does. */
	private static final class Shortcut {

		private final String[] keys;
		private final String description;

		private Shortcut(String[] keys, String description) {
			this.keys = keys;
			this.description = description;
		}
	}

	/** A titled group of rows. */
	private static final class Section {

		private final String title;
		private final Shortcut[] rows;

		private Section(String title, Shortcut[] rows) {
			this.title = title;
			this.rows = rows;
		}
	}

	private static Shortcut row(String description, String... keys) {
		return new Shortcut(keys, description);
	}

	private static final Section[] LEFT_COLUMN = {
		new Section("DRAWING", new Shortcut[] {
			row("Draw", "LMB"),
			row("Switch brush and line", "B"),
			row("Draw a square", "W"),
			row("Pick a colour off the canvas", "I"),
			row("Open the colour wheel", "C"),
			row("Flood fill, drag a swatch in", "Swatch")
		}),
		new Section("PAGES", new Shortcut[] {
			row("Previous and next page", "[", "]"),
			row("Play on a loop", "P"),
			row("Add or remove a page", "+", "-")
		})
	};

	private static final Section[] RIGHT_COLUMN = {
		new Section("VIEW", new Shortcut[] {
			row("Zoom around the pointer", "Scroll"),
			row("Zoom", "RMB"),
			row("Move the canvas", "MMB", "Space"),
			row("Onion skin the page before", "O")
		}),
		new Section("FILE AND TOOLS", new Shortcut[] {
			row("Save the project", "Ctrl", "S"),
			row("Undo the last edit", "Ctrl", "Z"),
			row("Open the tool menu", "E"),
			row("Show this help", "H")
		})
	};

	// ---------------------------------------------------------------- layout

	private static final int W = 660;
	private static final int H = 486;
	private static final int MARGIN = 28;
	private static final int COLUMN_GAP = 36;
	private static final int COLUMN_W = (W - MARGIN * 2 - COLUMN_GAP) / 2;

	private static final int CONTENT_TOP = 108;
	private static final int ROW_H = 26;
	private static final int HEADER_H = 30;
	private static final int SECTION_GAP = 18;

	private static final int CAP_H = 20;
	private static final int CAP_GAP = 5;
	private static final int CAP_PADDING = 14;
	/** Descriptions all start here, so every column reads as one straight edge. */
	private static final int DESCRIPTION_X = 100;

	private static final Rectangle CLOSE = new Rectangle(MARGIN, H - 62, W - MARGIN * 2, 42);

	private int mouseX = -1;
	private int mouseY = -1;

	private transient Runnable onClose = () -> { };

	HelpScreen() {
		setPreferredSize(new Dimension(W, H));
		setBackground(Theme.INK);

		MouseAdapter mouse = new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent event) {
				if (CLOSE.contains(event.getX(), event.getY())) {
					onClose.run();
				}
			}

			@Override
			public void mouseMoved(MouseEvent event) {
				mouseX = event.getX();
				mouseY = event.getY();
				repaint();
			}

			@Override
			public void mouseExited(MouseEvent event) {
				mouseX = -1;
				mouseY = -1;
				repaint();
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
	}

	void setOnClose(Runnable listener) {
		onClose = listener;
	}

	// ---------------------------------------------------------------- painting

	@Override
	protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D painter = (Graphics2D) graphics;
		painter.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		painter.setColor(Theme.INK);
		painter.fillRect(0, 0, getWidth(), getHeight());
		paintBackdrop(painter);
		paintTitle(painter);

		paintColumn(painter, LEFT_COLUMN, MARGIN);
		paintColumn(painter, RIGHT_COLUMN, MARGIN + COLUMN_W + COLUMN_GAP);

		paintClose(painter);
	}

	/** The same faint pixel grid the start screen uses. */
	private void paintBackdrop(Graphics2D painter) {
		painter.setColor(Theme.INK_DEEP);
		for (int y = 0; y < getHeight(); y += 8) {
			for (int x = 0; x < getWidth(); x += 8) {
				painter.fillRect(x, y, 2, 2);
			}
		}
	}

	private void paintTitle(Graphics2D painter) {
		painter.setFont(Theme.font(Font.BOLD, 24));
		painter.setColor(Theme.CREAM);
		painter.drawString("Shortcuts", MARGIN, 52);

		painter.setFont(Theme.font(Font.PLAIN, 12));
		painter.setColor(Theme.MUTED);
		painter.drawString("Everything the editor can do, and how to reach it.", MARGIN, 72);

		painter.setColor(Theme.PEACH);
		painter.fillRect(MARGIN, 86, 44, 3);
	}

	/** Draws one column of titled sections, top to bottom. */
	private void paintColumn(Graphics2D painter, Section[] sections, int columnX) {
		int y = CONTENT_TOP;
		for (Section section : sections) {
			painter.setFont(Theme.font(Font.BOLD, 11));
			painter.setColor(Theme.PEACH);
			painter.drawString(section.title, columnX, y);
			y += HEADER_H - 12;

			for (Shortcut shortcut : section.rows) {
				paintRow(painter, shortcut, columnX, y);
				y += ROW_H;
			}
			y += SECTION_GAP;
		}
	}

	/** One row: the key caps, then the description to the right of them. */
	private void paintRow(Graphics2D painter, Shortcut shortcut, int columnX, int y) {
		int capX = columnX;
		painter.setFont(Theme.font(Font.BOLD, 11));
		for (String key : shortcut.keys) {
			capX = paintCap(painter, key, capX, y) + CAP_GAP;
		}

		painter.setFont(Theme.font(Font.PLAIN, 12));
		painter.setColor(Theme.CREAM);
		FontMetrics metrics = painter.getFontMetrics();
		int textX = columnX + DESCRIPTION_X;
		if (capX > textX) {
			textX = capX + 6;
		}
		painter.drawString(Theme.clip(painter, shortcut.description, columnX + COLUMN_W - textX),
				textX, y + metrics.getAscent() - 1);
	}

	/** A single key cap. Returns the x just past its right edge. */
	private int paintCap(Graphics2D painter, String label, int x, int y) {
		FontMetrics metrics = painter.getFontMetrics();
		int width = metrics.stringWidth(label) + CAP_PADDING;
		if (width < 24) {
			width = 24;
		}

		painter.setColor(Theme.INK_DEEP);
		painter.fillRect(x + 2, y + 2, width, CAP_H);
		painter.setColor(Theme.PANEL);
		painter.fillRect(x, y, width, CAP_H);

		painter.setColor(Theme.CREAM);
		painter.drawString(label, x + (width - metrics.stringWidth(label)) / 2,
				y + (CAP_H - metrics.getHeight()) / 2 + metrics.getAscent());
		return x + width;
	}

	private void paintClose(Graphics2D painter) {
		boolean hot = CLOSE.contains(mouseX, mouseY);
		Color face = Theme.FLAME;
		Color ink = Theme.CREAM;
		if (hot) {
			face = Theme.PEACH;
			ink = Theme.INK;
		}
		Theme.block(painter, CLOSE, face, Theme.INK_DEEP);
		painter.setFont(Theme.font(Font.BOLD, 16));
		painter.setColor(ink);
		Theme.centreIn(painter, "Got it", CLOSE);
	}
}
