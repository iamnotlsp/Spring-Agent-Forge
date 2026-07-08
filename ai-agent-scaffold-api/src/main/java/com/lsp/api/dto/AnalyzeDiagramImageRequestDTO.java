package com.lsp.api.dto;

import lombok.Data;

@Data
public class AnalyzeDiagramImageRequestDTO {

    private String agentId;

    private String userId;

    private String sessionId;

    private String requestId;

    private String message;

    /**
     * 前端上传图片后转成 Base64 Data URL。
     * 示例：data:image/png;base64,iVBORw0KGgo...
     */
    private String imageDataUrl;

}
