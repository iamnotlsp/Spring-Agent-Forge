package com.lsp.domain.agent.model.valobj.harness;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrawioDiagnosticResult {

    //输出是否有效
    private boolean valid;

    //模型返回的业务类型
    private String outputType;

    //模型的原始输出
    private String rawOutput;

    //从原始输出中提取并规范化后的 XML 内容
    private String normalizedXml;

    @Builder.Default
    private List<DrawioDiagnosticIssue> issues = new ArrayList<>();

    //添加一个诊断问题
    public void addIssue(DrawioIssueType type, String message) {
        addIssue(type, message, null);
    }

    public void addIssue(DrawioIssueType type, String message, String location) {
        issues.add(DrawioDiagnosticIssue.builder()
                .type(type)
                .message(message)
                .location(location)
                .build());
        valid = false;
    }

}
