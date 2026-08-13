import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;

/**
 * Writes the document out in one of three formats. Every file is written to the
 * working directory and silently replaces an existing file of the same name.
 */
public class Saving {

	/** Each canvas pixel becomes a block of this many image pixels in the exported PNGs. */
	private static final int PNG_SCALE = 20;

	private final ArrayList<Page> pages;
	private final int canvasSize;
	private final String name;
	private final Color[] palette;

	Saving(ArrayList<Page> pages, int canvasSize, String name, Color[] palette) {
		this.pages = pages;
		this.canvasSize = canvasSize;
		this.name = name;
		this.palette = Arrays.copyOf(palette, palette.length);
	}

	/** Writes one PNG per page, named <name>0.png, <name>1.png and so on. */
	public void savePng() {
		for (int p = 0; p < pages.size(); p++) {
			BufferedImage image = new BufferedImage(canvasSize * PNG_SCALE, canvasSize * PNG_SCALE,
					BufferedImage.TYPE_INT_RGB);
			Graphics2D g = image.createGraphics();
			for (int row = 0; row < canvasSize; row++) {
				for (int col = 0; col < canvasSize; col++) {
					g.setColor(pages.get(p).pixels[row * canvasSize + col]);
					g.fillRect(col * PNG_SCALE, row * PNG_SCALE, PNG_SCALE, PNG_SCALE);
				}
			}
			g.dispose();

			File output = new File(name + p + ".png");
			try {
				ImageIO.write(image, "png", output);
				System.out.println("Saved " + output.getName());
			} catch (IOException e) {
				System.out.println("Could not write " + output.getName() + ": " + e.getMessage());
			}
		}
	}

	/**
	 * Writes the whole document as whitespace separated text: the name, the page
	 * count, the canvas size, 16 palette RGB triples, then an RGB triple for every
	 * pixel of every page in turn. {@link Panel#load} reads this format back.
	 * The name must not contain spaces or the header no longer parses.
	 */
	public void savePixies() {
		File output = new File(name + ".pixies");
		try (FileWriter out = new FileWriter(output)) {
			out.write(name + " " + pages.size() + " " + canvasSize + " ");
			for (Color c : palette) {
				out.write(c.getRed() + " " + c.getGreen() + " " + c.getBlue() + " ");
			}
			for (Page page : pages) {
				for (Color c : page.pixels) {
					out.write(c.getRed() + " " + c.getGreen() + " " + c.getBlue() + " ");
				}
			}
			System.out.println("Saved " + output.getName());
		} catch (IOException e) {
			System.out.println("Could not write " + output.getName() + ": " + e.getMessage());
		}
	}

	/**
	 * Writes a one bit per pixel version: the name, the page count, then a 1 for
	 * every white pixel and a 0 for everything else. This is an export only
	 * format, nothing in the app reads it back.
	 */
	public void saveRona() {
		File output = new File(name + ".rona");
		try (FileWriter out = new FileWriter(output)) {
			out.write(name + " " + pages.size() + " ");
			for (Page page : pages) {
				for (Color c : page.pixels) {
					out.write(Color.WHITE.equals(c) ? "1 " : "0 ");
				}
			}
			System.out.println("Saved " + output.getName());
		} catch (IOException e) {
			System.out.println("Could not write " + output.getName() + ": " + e.getMessage());
		}
	}
}
