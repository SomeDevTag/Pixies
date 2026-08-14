import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/**
 * A real colour wheel: hue runs around the circle, saturation runs from the
 * middle out to the rim, and brightness comes from a separate slider.
 *
 * The wheel is rendered pixel by pixel into an image and cached, because that
 * only has to happen again when the brightness changes.
 *
 * Usage:
 *   ColorWheel wheel = new ColorWheel();
 *   wheel.setColor(startingColour);
 *   wheel.setOnChange(this::refresh);
 */
public class ColorWheel extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final int SIZE = 260;      // width and height of the wheel
	private static final int PADDING = 10;
	private static final int MARKER_RADIUS = 6;

	private float hue = 0f;
	private float saturation = 0f;
	private float brightness = 1f;

	private transient BufferedImage cached;
	private float cachedBrightness = -1f;

	/** Called whenever the user picks a new colour. */
	private transient Runnable onChange = () -> { };

	ColorWheel() {
		setPreferredSize(new Dimension(SIZE + PADDING * 2, SIZE + PADDING * 2));
		setOpaque(false);

		MouseAdapter picker = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				pickAt(e.getX(), e.getY());
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				pickAt(e.getX(), e.getY());
			}
		};
		addMouseListener(picker);
		addMouseMotionListener(picker);
	}

	void setOnChange(Runnable listener) {
		onChange = listener;
	}

	// ---------------------------------------------------------------- colour

	Color getColor() {
		return Color.getHSBColor(hue, saturation, brightness);
	}

	/** Moves the wheel to show an existing colour, without firing a change. */
	void setColor(Color c) {
		float[] components = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
		hue = components[0];
		saturation = components[1];
		brightness = components[2];
		repaint();
	}

	float getHue() {
		return hue;
	}

	float getSaturation() {
		return saturation;
	}

	float getBrightness() {
		return brightness;
	}

	void setBrightness(float value) {
		brightness = Math.max(0f, Math.min(1f, value));
		repaint();
		onChange.run();
	}

	/**
	 * Turns a click into a hue and a saturation. Clicks outside the rim are pulled
	 * back onto it, so dragging off the edge keeps picking instead of stopping.
	 */
	private void pickAt(int x, int y) {
		int radius = SIZE / 2;
		double offsetX = x - (PADDING + radius);
		double offsetY = y - (PADDING + radius);
		double distance = Math.hypot(offsetX, offsetY);

		// atan2 gives -PI..PI measured anticlockwise, hue wants 0..1 clockwise.
		double angle = Math.atan2(offsetY, offsetX);
		hue = (float) ((angle / (2 * Math.PI) + 1.0) % 1.0);
		saturation = (float) Math.min(1.0, distance / radius);

		repaint();
		onChange.run();
	}

	// ---------------------------------------------------------------- painting

	/** Builds the wheel image for the current brightness. */
	private BufferedImage render() {
		if (cached != null && cachedBrightness == brightness) {
			return cached;
		}

		BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		int radius = SIZE / 2;
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				double offsetX = x - radius;
				double offsetY = y - radius;
				double distance = Math.hypot(offsetX, offsetY);
				if (distance > radius) {
					continue; // leave the corners transparent
				}
				float hueAt = (float) ((Math.atan2(offsetY, offsetX) / (2 * Math.PI) + 1.0) % 1.0);
				float saturationAt = (float) (distance / radius);
				image.setRGB(x, y, Color.getHSBColor(hueAt, saturationAt, brightness).getRGB() | 0xFF000000);
			}
		}

		cached = image;
		cachedBrightness = brightness;
		return image;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D graphics = (Graphics2D) g;
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		graphics.drawImage(render(), PADDING, PADDING, null);

		int radius = SIZE / 2;
		graphics.setStroke(new BasicStroke(1));
		graphics.setColor(new Color(0, 0, 0, 60));
		graphics.drawOval(PADDING, PADDING, SIZE, SIZE);

		// Marker showing where the current colour sits.
		double angle = hue * 2 * Math.PI;
		int markerX = (int) (PADDING + radius + Math.cos(angle) * saturation * radius);
		int markerY = (int) (PADDING + radius + Math.sin(angle) * saturation * radius);

		graphics.setStroke(new BasicStroke(2));
		Color markerColor = Color.WHITE;
		if (brightness > 0.5f) {
			markerColor = Color.BLACK;
		}
		graphics.setColor(markerColor);
		graphics.drawOval(markerX - MARKER_RADIUS, markerY - MARKER_RADIUS,
				MARKER_RADIUS * 2, MARKER_RADIUS * 2);
	}
}
