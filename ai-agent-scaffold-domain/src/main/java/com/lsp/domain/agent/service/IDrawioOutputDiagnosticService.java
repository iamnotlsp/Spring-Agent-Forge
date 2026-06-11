package com.lsp.domain.agent.service;

import com.lsp.domain.agent.model.valobj.harness.DrawioDiagnosticResult;

/**
 * @description 画图agent结果诊断接口
 * @author 林善鹏
 * @date 2026-06-11 16:01
 */
public interface IDrawioOutputDiagnosticService {

    DrawioDiagnosticResult diagnose(String rawOutput);

}
