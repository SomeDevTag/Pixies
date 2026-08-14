import java.awt.Color;
import java.util.Arrays;

/**
 * One frame of the animation, a square grid of pixel colours stored row by row.
 *
 * Usage:
 *   Page page = new Page(canvasSize);
 *   page.pixels[y * canvasSize + x] = colour;
 */
public class Page {

	/** The pixel at (x, y) lives at index y * size + x. */
	public final Color[] pixels;

	Page(int size) {
		pixels = new Color[size * size];
		Arrays.fill(pixels, Color.WHITE);
	}
}
