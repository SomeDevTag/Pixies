import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

/**
 * Writes the document out. Everything lands in the exports folder and silently
 * replaces an existing file of the same name.
 *
 * Usage:
 *   Saving saver = new Saving(pages, canvasSize, name, palette);
 *   saver.savePng(scale: 20);
 *   saver.saveGif(scale: 20, delayMs: 150);
 */
public class Saving {

	/** Everything the editor saves goes in here, next to wherever the app was run from. */
	private static final File EXPORT_DIR = new File("exports");

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

	/**
	 * Where a given file should be written, creating the exports folder the first
	 * time something is saved. If the folder cannot be created we fall back to the
	 * working directory, since losing the save entirely would be worse.
	 */
	private static File output(String fileName) {
		if (EXPORT_DIR.isDirectory() || EXPORT_DIR.mkdirs()) {
			return new File(EXPORT_DIR, fileName);
		}
		System.out.println("Could not create the " + EXPORT_DIR.getName()
				+ " folder, saving to the working directory instead");
		return new File(fileName);
	}

	/** The folder saves go to, so the main menu can open the file chooser there. */
	public static File exportDir() {
		return EXPORT_DIR;
	}

	// ================================================================ images

	/** Draws one page into an image, blown up so each canvas pixel is scale x scale. */
	private BufferedImage renderPage(int index, int scale) {
		BufferedImage image = new BufferedImage(canvasSize * scale, canvasSize * scale,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		drawPage(g, index, scale, 0, 0);
		g.dispose();
		return image;
	}

	/** Paints a page onto an existing surface at the given offset. */
	private void drawPage(Graphics2D g, int index, int scale, int offsetX, int offsetY) {
		Color[] pixels = pages.get(index).pixels;
		for (int row = 0; row < canvasSize; row++) {
			for (int col = 0; col < canvasSize; col++) {
				g.setColor(pixels[row * canvasSize + col]);
				g.fillRect(offsetX + col * scale, offsetY + row * scale, scale, scale);
			}
		}
	}

	/** Writes one PNG per page, named <name>0.png, <name>1.png and so on. */
	public void savePng(int scale) {
		for (int pageNumber = 0; pageNumber < pages.size(); pageNumber++) {
			File output = output(name + pageNumber + ".png");
			try {
				ImageIO.write(renderPage(pageNumber, scale), "png", output);
				System.out.println("Saved " + output.getPath());
			} catch (IOException e) {
				System.out.println("Could not write " + output.getPath() + ": " + e.getMessage());
			}
		}
	}

	/**
	 * Writes every page into a single image with the frames laid out side by side,
	 * which is the usual way to hand an animation to a game engine.
	 *
	 * Pass 0 for columns to get one long row, otherwise the frames wrap onto as many
	 * rows as they need.
	 */
	public void saveSheet(int scale, int columns) {
		int perRow = pages.size();
		if (columns > 0) {
			perRow = Math.min(columns, pages.size());
		}
		int rows = (pages.size() + perRow - 1) / perRow;
		int frame = canvasSize * scale;

		BufferedImage sheet = new BufferedImage(perRow * frame, rows * frame,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = sheet.createGraphics();
		// Anything past the last frame stays white rather than black.
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
		for (int pageNumber = 0; pageNumber < pages.size(); pageNumber++) {
			drawPage(g, pageNumber, scale, (pageNumber % perRow) * frame, (pageNumber / perRow) * frame);
		}
		g.dispose();

		File output = output(name + "_sheet.png");
		try {
			ImageIO.write(sheet, "png", output);
			System.out.println("Saved " + output.getPath() + "  (" + pages.size()
					+ " frames, " + perRow + " per row)");
		} catch (IOException e) {
			System.out.println("Could not write " + output.getPath() + ": " + e.getMessage());
		}
	}

	/**
	 * Writes the whole animation as a looping GIF, one page per frame.
	 *
	 * The per frame delay and the loop flag both live in GIF metadata rather than in
	 * the pixels, so each frame gets a Graphic Control Extension node, and the first
	 * frame additionally carries the Netscape extension that says "repeat forever".
	 */
	public void saveGif(int scale, int delayMs) {
		File output = output(name + ".gif");
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("gif");
		if (!writers.hasNext()) {
			System.out.println("No GIF writer available on this Java installation");
			return;
		}
		ImageWriter writer = writers.next();

		try (ImageOutputStream out = ImageIO.createImageOutputStream(output)) {
			writer.setOutput(out);
			writer.prepareWriteSequence(null);

			for (int pageNumber = 0; pageNumber < pages.size(); pageNumber++) {
				BufferedImage frame = renderPage(pageNumber, scale);
				ImageWriteParam params = writer.getDefaultWriteParam();
				IIOMetadata metadata = writer.getDefaultImageMetadata(
						new ImageTypeSpecifier(frame), params);
				configureFrame(metadata, delayMs, pageNumber == 0);
				writer.writeToSequence(new IIOImage(frame, null, metadata), params);
			}

			writer.endWriteSequence();
			System.out.println("Saved " + output.getPath() + "  (" + pages.size()
					+ " frames at " + delayMs + "ms)");
		} catch (IOException e) {
			System.out.println("Could not write " + output.getPath() + ": " + e.getMessage());
		} finally {
			writer.dispose();
		}
	}

	/** Sets the frame delay, and on the first frame the loop forever flag. */
	private static void configureFrame(IIOMetadata metadata, int delayMs, boolean firstFrame)
			throws IOException {
		String format = metadata.getNativeMetadataFormatName();
		IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);

		IIOMetadataNode control = child(root, "GraphicControlExtension");
		control.setAttribute("disposalMethod", "none");
		control.setAttribute("userInputFlag", "FALSE");
		control.setAttribute("transparentColorFlag", "FALSE");
		control.setAttribute("transparentColorIndex", "0");
		// GIF stores the delay in hundredths of a second, so 150ms becomes 15.
		control.setAttribute("delayTime", String.valueOf(Math.max(1, delayMs / 10)));

		if (firstFrame) {
			IIOMetadataNode extensions = child(root, "ApplicationExtensions");
			IIOMetadataNode netscape = new IIOMetadataNode("ApplicationExtension");
			netscape.setAttribute("applicationID", "NETSCAPE");
			netscape.setAttribute("authenticationCode", "2.0");
			// 1 means a looping extension, then the repeat count as two bytes. 0 is forever.
			netscape.setUserObject(new byte[] { 1, 0, 0 });
			extensions.appendChild(netscape);
		}

		metadata.setFromTree(format, root);
	}

	/** Finds a named child of the metadata tree, adding it if it isn't there yet. */
	private static IIOMetadataNode child(IIOMetadataNode parent, String name) {
		for (int i = 0; i < parent.getLength(); i++) {
			if (parent.item(i).getNodeName().equalsIgnoreCase(name)) {
				return (IIOMetadataNode) parent.item(i);
			}
		}
		IIOMetadataNode added = new IIOMetadataNode(name);
		parent.appendChild(added);
		return added;
	}

	// ================================================================ text formats

	/**
	 * Writes the whole document as whitespace separated text: the name, the page
	 * count, the canvas size, 16 palette RGB triples, then an RGB triple for every
	 * pixel of every page in turn. {@link Panel#load} reads this format back.
	 * The name must not contain spaces or the header no longer parses.
	 */
	public void savePixies() {
		File output = output(name + ".pixies");
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
			System.out.println("Saved " + output.getPath());
		} catch (IOException e) {
			System.out.println("Could not write " + output.getPath() + ": " + e.getMessage());
		}
	}

	/**
	 * Writes a one bit per pixel version: the name, the page count, then a 1 for
	 * every white pixel and a 0 for everything else. This is an export only
	 * format, nothing in the app reads it back.
	 */
	public void saveRona() {
		File output = output(name + ".rona");
		try (FileWriter out = new FileWriter(output)) {
			out.write(name + " " + pages.size() + " ");
			for (Page page : pages) {
				for (Color pixel : page.pixels) {
					if (Color.WHITE.equals(pixel)) {
						out.write("1 ");
					}
					else {
						out.write("0 ");
					}
				}
			}
			System.out.println("Saved " + output.getPath());
		} catch (IOException e) {
			System.out.println("Could not write " + output.getPath() + ": " + e.getMessage());
		}
	}
}
