import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 * The colour wheel window. Applying overwrites whichever swatch is currently
 * selected in the panel.
 *
 * Note: do not import java.awt.Panel here. A single type import would shadow this
 * project's own Panel class, which is what previously left Apply wired to nothing.
 *
 * Usage:
 *   new ColorPicker(panel, currentColor).setVisible(true);
 */
public class ColorPicker extends JFrame {

	private static final long serialVersionUID = 1L;

	private final Panel panel;
	private final ColorWheel wheel = new ColorWheel();
	private final BrightnessBar brightness = new BrightnessBar();
	private final Preview preview = new Preview();
	private final JLabel readout = new JLabel();

	ColorPicker(Panel panel, Color initialColor) {
		this.panel = panel;
		Color opening = initialColor;
		if (opening == null) {
			opening = Color.WHITE;
		}
		buildUi(opening);
	}

	private void buildUi(Color initialColor) {
		setTitle("Pixies - Color Wheel");
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setResizable(false);

		wheel.setColor(initialColor);
		wheel.setOnChange(this::refresh);
		brightness.onChange = value -> {
			wheel.setBrightness(value);
			refresh();
		};

		JLabel brightLabel = new JLabel("Bright");
		brightLabel.setFont(Theme.font(Font.BOLD, 11));
		brightLabel.setForeground(Theme.MUTED);
		brightLabel.setAlignmentX(CENTER_ALIGNMENT);

		JPanel side = new JPanel(new BorderLayout(0, 6));
		side.setOpaque(false);
		side.add(brightLabel, BorderLayout.NORTH);
		side.add(brightness, BorderLayout.CENTER);

		JPanel middle = new JPanel(new BorderLayout(14, 0));
		middle.setOpaque(false);
		middle.setBorder(BorderFactory.createEmptyBorder(14, 14, 6, 14));
		middle.add(wheel, BorderLayout.CENTER);
		middle.add(side, BorderLayout.EAST);

		// The readout gets a row of its own. It used to share one with the buttons,
		// which meant a long hex value ran straight underneath them.
		readout.setFont(Theme.font(Font.PLAIN, 13));
		readout.setForeground(Theme.CREAM);

		JPanel readoutRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
		readoutRow.setOpaque(false);
		readoutRow.add(preview);
		readoutRow.add(readout);

		JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		buttonRow.setOpaque(false);
		buttonRow.add(flatButton("Cancel", Theme.PANEL, Theme.CREAM, e -> dispose()));
		buttonRow.add(flatButton("Apply", Theme.PEACH, Theme.INK, e -> {
			panel.applyPickedColor(wheel.getColor());
			dispose();
		}));

		JPanel bottom = new JPanel(new BorderLayout(0, 12));
		bottom.setOpaque(false);
		bottom.setBorder(BorderFactory.createEmptyBorder(6, 14, 14, 14));
		bottom.add(readoutRow, BorderLayout.NORTH);
		bottom.add(buttonRow, BorderLayout.SOUTH);

		JPanel content = new JPanel(new BorderLayout());
		content.setBackground(Theme.INK);
		content.add(middle, BorderLayout.CENTER);
		content.add(bottom, BorderLayout.SOUTH);
		setContentPane(content);

		refresh();
		pack();
		setLocationRelativeTo(null);
	}

	/** A flat themed button, since the native ones ignore the palette. */
	private static JButton flatButton(String text, Color face, Color ink,
			java.awt.event.ActionListener action) {
		JButton button = new JButton(text);
		button.setFont(Theme.font(Font.BOLD, 13));
		button.setBackground(face);
		button.setForeground(ink);
		button.setFocusPainted(false);
		button.setBorderPainted(false);
		button.setOpaque(true);
		button.setPreferredSize(new Dimension(110, 34));
		button.addActionListener(action);
		return button;
	}

	/** Keeps the preview, the readout and the brightness bar in step with the wheel. */
	private void refresh() {
		Color c = wheel.getColor();
		preview.color = c;
		preview.repaint();
		brightness.setHue(wheel.getHue(), wheel.getSaturation(), wheel.getBrightness());
		readout.setText(String.format("R %d   G %d   B %d      #%02X%02X%02X",
				c.getRed(), c.getGreen(), c.getBlue(), c.getRed(), c.getGreen(), c.getBlue()));
	}

	/** A filled square showing the colour Apply would use. */
	private static class Preview extends JPanel {
		private static final long serialVersionUID = 1L;
		transient Color color = Color.WHITE;

		Preview() {
			setPreferredSize(new Dimension(44, 44));
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.setColor(Theme.INK_DEEP);
			g.fillRect(Theme.SHADOW, Theme.SHADOW, getWidth() - Theme.SHADOW, getHeight() - Theme.SHADOW);
			g.setColor(color);
			g.fillRect(0, 0, getWidth() - Theme.SHADOW, getHeight() - Theme.SHADOW);
		}
	}

	/**
	 * A vertical brightness strip, running from the chosen hue at full brightness
	 * down to black. Hand painted so it matches the rest of the app rather than
	 * looking like a stock slider.
	 */
	private static class BrightnessBar extends JPanel {
		private static final long serialVersionUID = 1L;

		private static final int BAR_W = 26;

		private float hue = 0f;
		private float saturation = 0f;
		private float value = 1f;

		transient java.util.function.Consumer<Float> onChange = v -> { };

		BrightnessBar() {
			setPreferredSize(new Dimension(BAR_W + Theme.SHADOW, 240));
			setOpaque(false);
			MouseAdapter drag = new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					pick(e.getY());
				}

				@Override
				public void mouseDragged(MouseEvent e) {
					pick(e.getY());
				}
			};
			addMouseListener(drag);
			addMouseMotionListener(drag);
		}

		void setHue(float h, float s, float v) {
			hue = h;
			saturation = s;
			value = v;
			repaint();
		}

		private void pick(int y) {
			int h = getHeight();
			value = Math.max(0f, Math.min(1f, 1f - (y / (float) h)));
			repaint();
			onChange.accept(value);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D graphics = (Graphics2D) g;
			int h = getHeight();

			graphics.setColor(Theme.INK_DEEP);
			graphics.fillRect(Theme.SHADOW, Theme.SHADOW, BAR_W, h);

			// One row per pixel, brightest at the top.
			for (int y = 0; y < h; y++) {
				float v = 1f - (y / (float) h);
				graphics.setColor(Color.getHSBColor(hue, saturation, v));
				graphics.fillRect(0, y, BAR_W, 1);
			}

			int markerY = Math.round((1f - value) * (h - 1));
			graphics.setColor(Theme.CREAM);
			graphics.fillRect(-2, markerY - 1, BAR_W + 4, 3);
			graphics.setColor(Theme.INK_DEEP);
			graphics.drawRect(-2, markerY - 2, BAR_W + 3, 4);
		}
	}
}
