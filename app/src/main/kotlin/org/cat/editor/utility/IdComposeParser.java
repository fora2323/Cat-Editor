package org.cat.editor.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IdComposeParser {
    public static ComposeNode.Layout parse(String rawCode) {
        String code = rawCode
            .replaceAll("/\\*([\\s\\S]*?)\\*/", "")
            .replaceAll("//.*", "");

        ComposeNode.Layout root = new ComposeNode.Layout("PreviewRoot", "#FFFFFF");
        Stack<ComposeNode.Layout> stack = new Stack<>();
        stack.push(root);

        ComposeNode.Layout lastCreatedLayout = null;
        ComposeNode.Leaf lastCreatedLeaf = null;

        Pattern p = Pattern.compile("\"([^\"]*)\"|\\b([A-Z][a-zA-Z0-9_]+)\\b(?=\\s*[\\(\\{])|(\\{|\\})");
        Matcher m = p.matcher(code);

        while (m.find()) {
            String strGrp = m.group(1);
            String compGrp = m.group(2);
            String braceGrp = m.group(3);
            ComposeNode.Layout currentParent = stack.peek();

            if (strGrp != null) {
                if (lastCreatedLeaf != null) lastCreatedLeaf.text = strGrp;
            } else if (braceGrp != null) {
                if (braceGrp.equals("{")) {
                    if (lastCreatedLayout != null) stack.push(lastCreatedLayout);
                    else stack.push(currentParent);
                    lastCreatedLayout = null;
                    lastCreatedLeaf = null;
                } else if (braceGrp.equals("}")) {
                    if (stack.size() > 1) stack.pop();
                    lastCreatedLayout = null;
                    lastCreatedLeaf = null;
                }
            } else if (compGrp != null) {
                boolean isLayout = true;
                String colorHex = "#757575";
                boolean isHorizontal = false;

                switch (compGrp) {
                    case "Column": case "LazyColumn":
                        colorHex = "#FF9800"; break;
                    case "Row": case "LazyRow":
                        colorHex = "#E040FB"; isHorizontal = true; break;
                    case "Box": case "Surface": case "Card":
                        colorHex = "#03A9F4"; break;
                    case "Button": case "OutlinedButton":
                        colorHex = "#00E676"; isHorizontal = true; break;
                    case "Text": case "Icon": case "Image": case "Spacer":
                        isLayout = false; colorHex = "#B0BEC5"; break;
                    default:
                        colorHex = "#F48FB1"; break;
                }

                if (isLayout) {
                    ComposeNode.Layout newLayout = new ComposeNode.Layout(compGrp, new ArrayList<>(), isHorizontal, colorHex);
                    currentParent.children.add(newLayout);
                    lastCreatedLayout = newLayout;
                } else {
                    ComposeNode.Leaf newLeaf = new ComposeNode.Leaf(compGrp, colorHex);
                    currentParent.children.add(newLeaf);
                    lastCreatedLeaf = newLeaf;
                }
            }
        }
        return root;
    }
}