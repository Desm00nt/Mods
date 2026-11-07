package org.sprautestudio.spraute_engine.ui.code;

import java.util.ArrayList;
import java.util.List;

public final class TextDocument {
    public static final int KIND_OTHER = 0;
    public static final int KIND_TYPE = 1;
    public static final int KIND_DELETE = 2;
    private final StringBuilder text = new StringBuilder();
    private int[] lineStarts = new int[]{0};
    private final List<TextDocument.Edit> undo = new ArrayList<>();
    private final List<TextDocument.Edit> redo = new ArrayList<>();
    private int version;
    private int savedVersion;
    private boolean breakGroup;
    private final List<Integer> groupStarts = new ArrayList<>();

    public TextDocument(String initial) {
        this.text.append(normalize(initial));
        this.reindex();
    }

    public static String normalize(String s) {
        return s.replace("\r\n", "\n").replace('\r', '\n').replace("\t", "    ");
    }

    private void reindex() {
        List<Integer> starts = new ArrayList<>();
        starts.add(0);

        for (int i = 0; i < this.text.length(); i++) {
            if (this.text.charAt(i) == '\n') {
                starts.add(i + 1);
            }
        }

        this.lineStarts = new int[starts.size()];

        for (int ix = 0; ix < this.lineStarts.length; ix++) {
            this.lineStarts[ix] = starts.get(ix);
        }
    }

    public String text() {
        return this.text.toString();
    }

    public int length() {
        return this.text.length();
    }

    public char charAt(int i) {
        return this.text.charAt(i);
    }

    public String substring(int from, int to) {
        return this.text.substring(Math.max(0, from), Math.min(this.text.length(), to));
    }

    public int lineCount() {
        return this.lineStarts.length;
    }

    public int lineStart(int line) {
        return this.lineStarts[Math.max(0, Math.min(this.lineStarts.length - 1, line))];
    }

    public int lineEnd(int line) {
        return line + 1 < this.lineStarts.length ? this.lineStarts[line + 1] - 1 : this.text.length();
    }

    public String line(int line) {
        return this.text.substring(this.lineStart(line), this.lineEnd(line));
    }

    public int lineOf(int offset) {
        int lo = 0;
        int hi = this.lineStarts.length - 1;

        while (lo < hi) {
            int mid = lo + hi + 1 >>> 1;
            if (this.lineStarts[mid] <= offset) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }

        return lo;
    }

    public int colOf(int offset) {
        return offset - this.lineStart(this.lineOf(offset));
    }

    public int offsetOf(int line, int col) {
        line = Math.max(0, Math.min(this.lineCount() - 1, line));
        return Math.min(this.lineStart(line) + Math.max(0, col), this.lineEnd(line));
    }

    public int version() {
        return this.version;
    }

    public boolean isDirty() {
        return this.version != this.savedVersion;
    }

    public void markSaved() {
        this.savedVersion = this.version;
    }

    public void replace(int from, int to, String inserted, int caretBefore, int anchorBefore, int caretAfter, int anchorAfter, int kind) {
        from = Math.max(0, Math.min(this.text.length(), from));
        to = Math.max(from, Math.min(this.text.length(), to));
        inserted = normalize(inserted);
        String removed = this.text.substring(from, to);
        if (!removed.isEmpty() || !inserted.isEmpty()) {
            this.text.replace(from, to, inserted);
            this.reindex();
            this.version++;
            long now = System.currentTimeMillis();
            TextDocument.Edit e = new TextDocument.Edit(from, removed, inserted, caretBefore, anchorBefore, caretAfter, anchorAfter, now, kind);
            boolean merge = false;
            if (!this.breakGroup && !this.undo.isEmpty() && kind != 0) {
                TextDocument.Edit last = this.undo.get(this.undo.size() - 1);
                boolean sameKind = last.kind == kind && now - last.time < 1200L;
                boolean contiguous = kind == 1
                    ? last.offset + last.inserted.length() == from && !inserted.contains("\n")
                    : from + removed.length() == last.offset || from == last.offset;
                boolean wordBoundary = kind == 1 && inserted.equals(" ") && !last.inserted.endsWith(" ");
                merge = sameKind && contiguous && !wordBoundary;
            }

            if (!merge) {
                this.groupStarts.add(this.undo.size());
            }

            this.undo.add(e);
            this.redo.clear();
            this.breakGroup = false;
        }
    }

    public void breakUndoGroup() {
        this.breakGroup = true;
    }

    public boolean canUndo() {
        return !this.undo.isEmpty();
    }

    public boolean canRedo() {
        return !this.redo.isEmpty();
    }

    public int[] undo() {
        if (this.undo.isEmpty()) {
            return null;
        } else {
            int start = this.groupStarts.isEmpty() ? 0 : this.groupStarts.remove(this.groupStarts.size() - 1);
            int[] sel = null;
            List<TextDocument.Edit> group = new ArrayList<>();

            for (int i = this.undo.size() - 1; i >= start; i--) {
                TextDocument.Edit e = this.undo.remove(i);
                this.text.replace(e.offset, e.offset + e.inserted.length(), e.removed);
                group.add(0, e);
                sel = new int[]{e.caretBefore, e.anchorBefore};
            }

            this.redo.addAll(group);
            this.redo.add(null);
            this.reindex();
            this.version++;
            this.breakGroup = true;
            return sel;
        }
    }

    public int[] redo() {
        if (this.redo.isEmpty()) {
            return null;
        } else {
            this.redo.remove(this.redo.size() - 1);
            int start = this.redo.size();

            while (start > 0 && this.redo.get(start - 1) != null) {
                start--;
            }

            int[] sel = null;
            this.groupStarts.add(this.undo.size());
            List<TextDocument.Edit> group = new ArrayList<>(this.redo.subList(start, this.redo.size()));
            this.redo.subList(start, this.redo.size()).clear();

            for (TextDocument.Edit e : group) {
                this.text.replace(e.offset, e.offset + e.removed.length(), e.inserted);
                this.undo.add(e);
                sel = new int[]{e.caretAfter, e.anchorAfter};
            }

            this.reindex();
            this.version++;
            this.breakGroup = true;
            return sel;
        }
    }

    public void setText(String s, int caret) {
        this.replace(0, this.text.length(), s, caret, caret, Math.min(caret, normalize(s).length()), Math.min(caret, normalize(s).length()), 0);
    }

    public record Edit(int offset, String removed, String inserted, int caretBefore, int anchorBefore, int caretAfter, int anchorAfter, long time, int kind) {
    }
}
