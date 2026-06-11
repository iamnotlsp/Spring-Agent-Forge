package com.lsp.domain.agent.service.harness.inspector;

import com.lsp.domain.agent.model.valobj.harness.DrawioDiagnosticResult;
import com.lsp.domain.agent.model.valobj.harness.DrawioIssueType;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.HashSet;
import java.util.Set;

/**
 * @description  Draw.io XML 结构规则检查
 * @author 林善鹏
 * @date 2026-06-11 16:18
 */
@Component
public class DrawioXmlInspector {

    public void inspect(Document document, DrawioDiagnosticResult result) {
        Element documentElement = document.getDocumentElement();
        if (null == documentElement || !"mxfile".equals(documentElement.getTagName())) {
            result.addIssue(DrawioIssueType.INCOMPLETE_MXFILE, "Draw.io XML must use mxfile as the document root.", "mxfile");
            return;
        }

        if (document.getElementsByTagName("diagram").getLength() == 0) {
            result.addIssue(DrawioIssueType.MISSING_DIAGRAM, "Draw.io XML is missing diagram element.", "diagram");
        }

        if (document.getElementsByTagName("mxGraphModel").getLength() == 0) {
            result.addIssue(DrawioIssueType.MISSING_GRAPH_MODEL, "Draw.io XML is missing mxGraphModel element.", "mxGraphModel");
        }

        if (document.getElementsByTagName("root").getLength() == 0) {
            result.addIssue(DrawioIssueType.MISSING_ROOT, "Draw.io XML is missing root element.", "root");
        }

        inspectCells(document, result);
    }

    private void inspectCells(Document document, DrawioDiagnosticResult result) {
        NodeList cellNodes = document.getElementsByTagName("mxCell");
        Set<String> ids = new HashSet<>();
        Set<String> duplicatedIds = new HashSet<>();
        boolean hasRootCell = false;
        boolean hasLayerCell = false;

        for (int i = 0; i < cellNodes.getLength(); i++) {
            Element cell = (Element) cellNodes.item(i);
            String id = cell.getAttribute("id");

            if ("0".equals(id)) {
                hasRootCell = true;
            }
            if ("1".equals(id)) {
                hasLayerCell = true;
            }

            if (id == null || id.isBlank()) {
                result.addIssue(DrawioIssueType.MISSING_ROOT_CELL, "mxCell is missing id.", "mxCell");
                continue;
            }

            if (!ids.add(id)) {
                duplicatedIds.add(id);
            }
        }

        if (!hasRootCell) {
            result.addIssue(DrawioIssueType.MISSING_ROOT_CELL, "Draw.io XML is missing mxCell id=\"0\".", "mxCell#0");
        }
        if (!hasLayerCell) {
            result.addIssue(DrawioIssueType.MISSING_ROOT_CELL, "Draw.io XML is missing mxCell id=\"1\".", "mxCell#1");
        }
        for (String duplicatedId : duplicatedIds) {
            result.addIssue(DrawioIssueType.DUPLICATE_ID, "Duplicate mxCell id found.", duplicatedId);
        }

        inspectNodeAndEdgeRules(cellNodes, ids, result);
    }

    private void inspectNodeAndEdgeRules(NodeList cellNodes, Set<String> ids, DrawioDiagnosticResult result) {
        for (int i = 0; i < cellNodes.getLength(); i++) {
            Element cell = (Element) cellNodes.item(i);
            String id = cell.getAttribute("id");

            if ("1".equals(cell.getAttribute("vertex")) && !hasGeometry(cell)) {
                result.addIssue(DrawioIssueType.MISSING_GEOMETRY, "Visible node is missing mxGeometry.", id);
            }

            if (!"1".equals(cell.getAttribute("edge"))) {
                continue;
            }

            String source = cell.getAttribute("source");
            String target = cell.getAttribute("target");

            if (source == null || source.isBlank()) {
                result.addIssue(DrawioIssueType.MISSING_EDGE_SOURCE, "Edge is missing source.", id);
            } else if (!ids.contains(source)) {
                result.addIssue(DrawioIssueType.BROKEN_EDGE_REFERENCE, "Edge source references a missing mxCell.", id + ".source");
            }

            if (target == null || target.isBlank()) {
                result.addIssue(DrawioIssueType.MISSING_EDGE_TARGET, "Edge is missing target.", id);
            } else if (!ids.contains(target)) {
                result.addIssue(DrawioIssueType.BROKEN_EDGE_REFERENCE, "Edge target references a missing mxCell.", id + ".target");
            }

            if (!hasRelativeGeometry(cell)) {
                result.addIssue(DrawioIssueType.MISSING_EDGE_GEOMETRY, "Edge is missing relative mxGeometry.", id);
            }
        }
    }

    private boolean hasGeometry(Element cell) {
        NodeList geometryNodes = cell.getElementsByTagName("mxGeometry");
        for (int i = 0; i < geometryNodes.getLength(); i++) {
            Element geometry = (Element) geometryNodes.item(i);
            if ("geometry".equals(geometry.getAttribute("as"))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRelativeGeometry(Element cell) {
        NodeList geometryNodes = cell.getElementsByTagName("mxGeometry");
        for (int i = 0; i < geometryNodes.getLength(); i++) {
            Element geometry = (Element) geometryNodes.item(i);
            if ("geometry".equals(geometry.getAttribute("as")) && "1".equals(geometry.getAttribute("relative"))) {
                return true;
            }
        }
        return false;
    }

}
