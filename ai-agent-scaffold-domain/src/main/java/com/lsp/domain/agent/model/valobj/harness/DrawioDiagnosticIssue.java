package com.lsp.domain.agent.model.valobj.harness;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrawioDiagnosticIssue {

    private DrawioIssueType type;

    private String message;

    private String location;

}
