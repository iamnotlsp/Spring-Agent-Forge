package com.lsp.domain.agent.service.armory.tool.plugin;

import com.google.adk.plugins.LoggingPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("myLogPlugin")
public class MyLogPlugin extends LoggingPlugin {
}
