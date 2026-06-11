package com.lsp.domain.agent.service.harness.inspector;

import com.lsp.domain.agent.model.valobj.harness.DrawioDiagnosticResult;
import com.lsp.domain.agent.model.valobj.harness.DrawioIssueType;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * @description 布局质量检查
 * @author 林善鹏
 * @date 2026-06-11 16:19
 */
@Component
public class DrawioLayoutInspector {

    public void inspect(Document document, DrawioDiagnosticResult result) {
        List<Box> boxes = collectBoxes(document);
        for (int i = 0; i < boxes.size(); i++) {
            for (int j = i + 1; j < boxes.size(); j++) {
                Box first = boxes.get(i);
                Box second = boxes.get(j);
                if (first.overlaps(second)) {
                    result.addIssue(DrawioIssueType.LAYOUT_OVERLAP, "Visible nodes overlap.", first.id + "," + second.id);
                }
            }
        }
    }

    private List<Box> collectBoxes(Document document) {
        List<Box> boxes = new ArrayList<>();
        NodeList cellNodes = document.getElementsByTagName("mxCell");
        for (int i = 0; i < cellNodes.getLength(); i++) {
            Element cell = (Element) cellNodes.item(i);
            if (!"1".equals(cell.getAttribute("vertex"))) {
                continue;
            }

            NodeList geometryNodes = cell.getElementsByTagName("mxGeometry");
            if (geometryNodes.getLength() == 0) {
                continue;
            }

            Element geometry = (Element) geometryNodes.item(0);
            Double x = parseDouble(geometry.getAttribute("x"));
            Double y = parseDouble(geometry.getAttribute("y"));
            Double width = parseDouble(geometry.getAttribute("width"));
            Double height = parseDouble(geometry.getAttribute("height"));

            if (null == x || null == y || null == width || null == height) {
                continue;
            }

            boxes.add(new Box(cell.getAttribute("id"), x, y, width, height));
        }
        return boxes;
    }

    private Double parseDouble(String value) {
        try {
            return value == null || value.isBlank() ? null : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record Box(String id, double x, double y, double width, double height) {

        private boolean overlaps(Box other) {
            return x < other.x + other.width
                    && x + width > other.x
                    && y < other.y + other.height
                    && y + height > other.y;
        }

    }

}
