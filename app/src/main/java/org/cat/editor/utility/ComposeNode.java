package org.cat.editor.utility;

import java.util.ArrayList;
import java.util.List;

public abstract class ComposeNode {
    public static class Layout extends ComposeNode {
        public String name;
        public List<ComposeNode> children;
        public boolean isHorizontal;
        public String colorHex;

        public Layout(String name, List<ComposeNode> children, boolean isHorizontal, String colorHex) {
            this.name = name;
            this.children = children != null ? children : new ArrayList<>();
            this.isHorizontal = isHorizontal;
            this.colorHex = colorHex != null ? colorHex : "#757575";
        }

        public Layout(String name) {
            this(name, new ArrayList<>(), false, "#757575");
        }

        public Layout(String name, String colorHex) {
            this(name, new ArrayList<>(), false, colorHex);
        }
    }

    public static class Leaf extends ComposeNode {
        public String text;
        public String colorHex;

        public Leaf(String text, String colorHex) {
            this.text = text;
            this.colorHex = colorHex != null ? colorHex : "#B0BEC5";
        }

        public Leaf(String text) {
            this(text, "#B0BEC5");
        }
    }
}