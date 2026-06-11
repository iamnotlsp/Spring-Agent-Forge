package com.lsp.domain.agent.service.harness;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lsp.domain.agent.model.valobj.harness.DrawioDiagnosticResult;
import com.lsp.domain.agent.model.valobj.harness.DrawioIssueType;
import com.lsp.domain.agent.service.IDrawioOutputDiagnosticService;
import com.lsp.domain.agent.service.harness.inspector.DrawioLayoutInspector;
import com.lsp.domain.agent.service.harness.inspector.DrawioXmlInspector;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import javax.annotation.Resource;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

@Service
public class DrawioOutputDiagnosticService implements IDrawioOutputDiagnosticService {

    @Resource
    private DrawioXmlInspector drawioXmlInspector;

    @Resource
    private DrawioLayoutInspector drawioLayoutInspector;


    @Override
    public DrawioDiagnosticResult diagnose(String rawOutput) {
        // 创建一份诊断报告，默认认为有效 valid=true
        DrawioDiagnosticResult result = DrawioDiagnosticResult.builder()
                .valid(true)
                .rawOutput(rawOutput)
                .build();

        if (rawOutput == null || rawOutput.isBlank()) {
            result.addIssue(DrawioIssueType.EMPTY_OUTPUT, "Agent output is empty.");
            return result;
        }

        String content = extractContent(rawOutput, result);
        if (content == null || content.isBlank()) {
            return result;
        }

        String xml = extractMxfile(content);
        if (xml == null || xml.isBlank()) {
            result.addIssue(DrawioIssueType.INCOMPLETE_MXFILE, "Output does not contain a complete mxfile document.");
            return result;
        }

        result.setNormalizedXml(xml);

        //把 XML 字符串解析成 DOM 文档
        Document document = parseXml(xml, result);
        if (document == null) {
            return result;
        }

        drawioXmlInspector.inspect(document, result);
        drawioLayoutInspector.inspect(document, result);
        result.setValid(result.getIssues() == null || result.getIssues().isEmpty());
        return result;
    }

    private String extractContent(String rawOutput, DrawioDiagnosticResult result) {
        String text = stripMarkdownFence(rawOutput.trim());

        try {
            JSONObject jsonObject = JSON.parseObject(text);
            String type = jsonObject.getString("type");
            result.setOutputType(type);

            if (type != null && !"drawio".equals(type) && !"user".equals(type)) {
                result.addIssue(DrawioIssueType.UNEXPECTED_OUTPUT_TYPE, "Unexpected output type.", type);
            }

            Object content = jsonObject.get("content");
            if (content == null) {
                result.addIssue(DrawioIssueType.MISSING_CONTENT, "JSON output is missing content.");
                return null;
            }

            return content instanceof String ? (String) content : JSON.toJSONString(content);
        } catch (Exception e) {
            result.addIssue(DrawioIssueType.INVALID_JSON, "Output is not valid JSON.");
            return text;
        }
    }

    private String stripMarkdownFence(String text) {
        String stripped = text;
        stripped = stripped.replaceFirst("(?is)^```(?:json|xml)?\\s*", "");
        stripped = stripped.replaceFirst("(?is)\\s*```$", "");
        return stripped.trim();
    }

    private String extractMxfile(String text) {
        String cleaned = text
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\/", "/")
                .trim();

        int start = cleaned.indexOf("<mxfile");
        int end = cleaned.indexOf("</mxfile>", Math.max(start, 0));
        if (start < 0 || end < 0 || end <= start) {
            return null;
        }

        return cleaned.substring(start, end + "</mxfile>".length()).trim();
    }

    /**
     * @description XML 解析和安全校验
     * @author 林善鹏
     * @date 2026-06-11 16:06
     */
    private Document parseXml(String xml, DrawioDiagnosticResult result) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(false);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (SAXParseException e) {
            result.addIssue(DrawioIssueType.INVALID_XML, "Draw.io XML is not well formed.", "line " + e.getLineNumber() + ", column " + e.getColumnNumber());
            result.addIssue(DrawioIssueType.BAD_XML_ESCAPING, "XML may contain unescaped attribute or label content.");
            return null;
        } catch (Exception e) {
            result.addIssue(DrawioIssueType.INVALID_XML, "Draw.io XML cannot be parsed.");
            return null;
        }
    }

}
