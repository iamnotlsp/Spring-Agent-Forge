package com.lsp.api.dto;

import lombok.Data;

@Data
public class ChatResponseDTO {

    private String type;
    private Object content;
    private String sessionId;
    private String requestId;
    private Long durationMs;
    private Integer costCredits;
    private Integer remainingCredits;

}
