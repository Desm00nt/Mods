package org.sprautestudio.spraute_engine.ui.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.sprautestudio.spraute_engine.ui.Icon;

public class TreeView extends ListView<TreeView.Row> {
    private final List<TreeView.Node> roots = new ArrayList<>();
    public Consumer<TreeView.Node> onOpen;
    public TreeView.NodeContext onNodeContext;

    public TreeView() {
        super(null);
        this.rowHeight = 17;
        this.painter = (c, row, x, y, w, h, sel, hov) -> {
            TreeView.Node n = row.node();
            int ix = x + 4 + row.depth() * 10;
            if (!n.isLeaf()) {
                c.icon(n.expanded ? Icon.CHEVRON_DOWN : Icon.CHEVRON_RIGHT, ix, y + (h - 9) / 2, -9933435);
            }

            ix += 11;
            if (n.icon != null) {
                c.icon(n.icon, ix, y + (h - 9) / 2, n.iconColor);
                ix += 13;
            }

            int badgeW = n.badge != null ? c.width(n.badge) + 6 : 0;
            c.textEllipsized(n.label, ix, y + (h - 8) / 2, w - (ix - x) - badgeW - 4, sel ? -1841682 : n.textColor);
            if (n.badge != null) {
                c.text(n.badge, x + w - badgeW, y + (h - 8) / 2, n.badgeColor);
            }
        };
        this.onActivate = row -> {
            TreeView.Node n = row.node();
            if (!n.isLeaf()) {
                n.expanded = !n.expanded;
                this.rebuild();
            } else if (this.onOpen != null) {
                this.onOpen.accept(n);
            }
        };
        this.onContext = (row, mx, my) -> {
            if (this.onNodeContext != null) {
                this.onNodeContext.open(row == null ? null : row.node(), mx, my);
            }
        };
    }

    public List<TreeView.Node> roots() {
        return this.roots;
    }

    public void setRoots(List<TreeView.Node> nodes) {
        this.roots.clear();
        this.roots.addAll(nodes);
        this.rebuild();
    }

    public void rebuild() {
        List<TreeView.Row> rows = new ArrayList<>();

        for (TreeView.Node n : this.roots) {
            this.flatten(n, 0, rows);
        }

        this.setItems(rows);
    }

    private void flatten(TreeView.Node n, int depth, List<TreeView.Row> out) {
        out.add(new TreeView.Row(n, depth));
        if (n.expanded) {
            for (TreeView.Node c : n.children) {
                this.flatten(c, depth + 1, out);
            }
        }
    }

    public TreeView.Node selectedNode() {
        TreeView.Row r = this.selectedItem();
        return r == null ? null : r.node();
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        boolean r = super.mousePressed(mx, my, button);
        TreeView.Row row = this.selectedItem();
        if (button == 0 && row != null && !row.node().isLeaf()) {
            int chevronX = this.x + 4 + row.depth() * 10;
            if (mx >= chevronX - 2 && mx <= chevronX + 11) {
                row.node().expanded = !row.node().expanded;
                this.rebuild();
            }
        }

        return r;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        TreeView.Row row = this.selectedItem();
        if (row != null && key == 262 && !row.node().isLeaf()) {
            row.node().expanded = true;
            this.rebuild();
            return true;
        } else if (row != null && key == 263 && !row.node().isLeaf()) {
            row.node().expanded = false;
            this.rebuild();
            return true;
        } else {
            return super.keyPressed(key, scan, mods);
        }
    }

    public interface Folder {
    }

    public static class Node {
        public String label;
        public Icon icon;
        public int iconColor = -6577995;
        public int textColor = -1841682;
        public String badge;
        public int badgeColor = -9933435;
        public Object data;
        public boolean expanded = true;
        public final List<TreeView.Node> children = new ArrayList<>();

        public Node(String label, Icon icon, Object data) {
            this.label = label;
            this.icon = icon;
            this.data = data;
        }

        public TreeView.Node add(TreeView.Node child) {
            this.children.add(child);
            return child;
        }

        public boolean isLeaf() {
            return this.children.isEmpty() && !(this.data instanceof TreeView.Folder);
        }
    }

    public interface NodeContext {
        void open(TreeView.Node var1, int var2, int var3);
    }

    public record Row(TreeView.Node node, int depth) {
    }
}
