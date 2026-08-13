import java.awt.Color;
import java.util.Arrays;

/** One frame of the animation: a square grid of pixel colors, stored row by row. */
public class Page {

	/** The pixel at (x, y) lives at index y * size + x. */
	public final Color[] pixels;

	Page(int size) {
		pixels = new Color[size * size];
		Arrays.fill(pixels, Color.WHITE);
	}
}
