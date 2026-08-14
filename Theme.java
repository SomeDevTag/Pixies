import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Arrays;

/**
 * The look of the whole app in one place: a plum ground with the editor's own
 * warm swatches as accents, flat blocks with hard offset shadows, and no
 * gradients or rounded corners anywhere.
 *
 * The start screen and the editor both paint from here so they cannot drift apart.
 *
 * Usage:
 *   graphics.setColor(Theme.CREAM);
 *   Theme.block(graphics, bounds, Theme.PANEL, Theme.INK_DEEP);
 *   Theme.centreIn(graphics, "Save", bounds);
 */
public final class Theme {

	private Theme() {
	}

	/** Ground the canvas floats on. */
	public static final Color INK = new Color(43, 35, 64, 255);

	/** Shadows, the toolbar strip, and anything that should read as a hole. */
	public static final Color INK_DEEP = new Color(30, 24, 48, 255);

	/** Raised surfaces: cards, menu rows, buttons. */
	public static final Color PANEL = new Color(61, 51, 85, 255);

	/** Recessed surfaces: text fields, unselected chips. */
	public static final Color PANEL_DARK = new Color(47, 39, 67, 255);

	public static final Color CREAM = new Color(255, 243, 224, 255);
	public static final Color MUTED = new Color(154, 143, 181, 255);

	// Accents, lifted straight out of the editor's default 16 colours.
	public static final Color FLAME = new Color(255, 69, 0, 255);
	public static final Color PEACH = new Color(255, 168, 0, 255);
	public static final Color MINT = new Color(126, 237, 86, 255);

	/** How far the hard shadow is offset. Never blurred. */
	public static final int SHADOW = 4;

	private static final boolean HAS_SEGOE = Arrays.asList(
			GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
			.contains("Segoe UI");

	/** Segoe UI where it exists, otherwise whatever sans the system has. */
	public static Font font(int style, int size) {
		String family = Font.SANS_SERIF;
		if (HAS_SEGOE) {
			family = "Segoe UI";
		}
		return new Font(family, style, size);
	}

	/** A flat block with a hard pixel shadow. */
	public static void block(Graphics2D graphics, Rectangle r, Color face, Color shadow) {
		graphics.setColor(shadow);
		graphics.fillRect(r.x + SHADOW, r.y + SHADOW, r.width, r.height);
		graphics.setColor(face);
		graphics.fillRect(r.x, r.y, r.width, r.height);
	}

	public static void block(Graphics2D graphics, int x, int y, int w, int h, Color face) {
		block(graphics, new Rectangle(x, y, w, h), face, INK_DEEP);
	}

	/** Paints a bitmap where '1' means draw, using the current colour. */
	public static void sprite(Graphics2D graphics, String[] rows, int x, int y, int scale) {
		for (int row = 0; row < rows.length; row++) {
			for (int col = 0; col < rows[row].length(); col++) {
				if (rows[row].charAt(col) == '1') {
					graphics.fillRect(x + col * scale, y + row * scale, scale, scale);
				}
			}
		}
	}

	/** Draws text centred horizontally on a point. */
	public static void centreString(Graphics2D graphics, String text, int centreX, int baseline) {
		graphics.drawString(text, centreX - graphics.getFontMetrics().stringWidth(text) / 2, baseline);
	}

	/** Draws text centred in a box, both across and down. */
	public static void centreIn(Graphics2D graphics, String text, Rectangle box) {
		java.awt.FontMetrics fm = graphics.getFontMetrics();
		graphics.drawString(text, box.x + (box.width - fm.stringWidth(text)) / 2,
				box.y + (box.height - fm.getHeight()) / 2 + fm.getAscent());
	}

	/** Shortens text with an ellipsis until it fits. */
	public static String clip(Graphics2D graphics, String text, int maxWidth) {
		if (graphics.getFontMetrics().stringWidth(text) <= maxWidth) {
			return text;
		}
		String cut = text;
		while (cut.length() > 1 && graphics.getFontMetrics().stringWidth(cut + "...") > maxWidth) {
			cut = cut.substring(0, cut.length() - 1);
		}
		return cut + "...";
	}
}
