import java.awt.Color;
import java.util.ArrayList;

/**
 * One step on the undo stack: the document as it stood just before an edit.
 *
 * The page list and the palette are shallow copies, so an entry costs little more
 * than a handful of references no matter how big the canvas is. That is only safe
 * because {@link Panel#beginEdit} never draws on a page that has been snapshotted,
 * it swaps in a fresh copy first and leaves the original here.
 */
public class UndoEntry {

	final ArrayList<Page> pages;
	final int pageIndex;
	final Color[] palette;

	UndoEntry(ArrayList<Page> pages, int pageIndex, Color[] palette) {
		this.pages = new ArrayList<>(pages);
		this.pageIndex = pageIndex;
		this.palette = palette.clone();
	}
}
