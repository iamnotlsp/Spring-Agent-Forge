package com.lsp.domain.agent.service.armory.tool.plugin;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.CallbackContext;
import com.google.adk.agents.InvocationContext;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.LlmResponse;
import com.google.adk.plugins.BasePlugin;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.genai.types.Content;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service("prometheusMetricsPlugin")
public class PrometheusMetricsPlugin extends BasePlugin {

    private final MeterRegistry meterRegistry;
    private final Map<String, Timer.Sample> runSamples = new ConcurrentHashMap<>();
    private final Map<String, Timer.Sample> modelSamples = new ConcurrentHashMap<>();
    private final Map<String, String> modelNames = new ConcurrentHashMap<>();
    private final Map<String, Timer.Sample> toolSamples = new ConcurrentHashMap<>();


    public PrometheusMetricsPlugin(MeterRegistry meterRegistry) {
        super("PrometheusMetricsPlugin");
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Maybe<Content> beforeRunCallback(InvocationContext invocationContext) {
        runSamples.put(invocationContext.invocationId(), Timer.start(meterRegistry));
        Counter.builder("ai_agent_invocations_total")
                .description("AI Agent invocation count")
                .tag("app", invocationContext.appName())
                .tag("agent", invocationContext.agent().name())
                .register(meterRegistry)
                .increment();
        return super.beforeRunCallback(invocationContext);
    }

    @Override
    public Completable afterRunCallback(InvocationContext invocationContext) {
        Timer.Sample sample = runSamples.remove(invocationContext.invocationId());
        if (sample != null) {
            sample.stop(Timer.builder("ai_agent_invocation_duration_seconds")
                    .description("AI Agent invocation duration")
                    .tag("app", invocationContext.appName())
                    .tag("agent", invocationContext.agent().name())
                    .register(meterRegistry));
        }
        return super.afterRunCallback(invocationContext);
    }

    @Override
    public Maybe<com.google.genai.types.Content> beforeAgentCallback(BaseAgent agent, CallbackContext callbackContext) {
        Counter.builder("ai_agent_steps_total")
                .description("AI Agent step count")
                .tag("agent", agent.name())
                .register(meterRegistry)
                .increment();
        return super.beforeAgentCallback(agent, callbackContext);
    }

    @Override
    public Maybe<LlmResponse> beforeModelCallback(CallbackContext callbackContext, LlmRequest llmRequest) {
        String key = callbackContext.invocationId();
        String model = llmRequest.model().orElse("unknown");
        modelSamples.put(key, Timer.start(meterRegistry));
        modelNames.put(key, model);

        Counter.builder("ai_agent_model_calls_total")
                .description("AI model call count")
                .tag("agent", callbackContext.agentName())
                .tag("model", model)
                .register(meterRegistry)
                .increment();

        return super.beforeModelCallback(callbackContext, llmRequest);
    }

    @Override
    public Maybe<LlmResponse> afterModelCallback(CallbackContext callbackContext, LlmResponse llmResponse) {
        String key = callbackContext.invocationId();
        Timer.Sample sample = modelSamples.remove(key);
        String model = Optional.ofNullable(modelNames.remove(key)).orElse("unknown");

        if (sample != null) {
            sample.stop(Timer.builder("ai_agent_model_call_duration_seconds")
                    .description("AI model call duration")
                    .tag("agent", callbackContext.agentName())
                    .tag("model", model)
                    .tag("status", "success")
                    .register(meterRegistry));
        }

        return super.afterModelCallback(callbackContext, llmResponse);
    }

    @Override
    public Maybe<LlmResponse> onModelErrorCallback(CallbackContext callbackContext, LlmRequest llmRequest, Throwable throwable) {
        String model = llmRequest.model().orElse("unknown");

        Counter.builder("ai_agent_model_errors_total")
                .description("AI model error count")
                .tag("agent", callbackContext.agentName())
                .tag("model", model)
                .tag("error", throwable.getClass().getSimpleName())
                .register(meterRegistry)
                .increment();

        return super.onModelErrorCallback(callbackContext, llmRequest, throwable);
    }

    @Override
    public Maybe<Map<String, Object>> beforeToolCallback(BaseTool tool, Map<String, Object> input, ToolContext toolContext) {
        toolSamples.put(tool.name(), Timer.start(meterRegistry));

        Counter.builder("ai_agent_tool_calls_total")
                .description("AI tool call count")
                .tag("tool", tool.name())
                .register(meterRegistry)
                .increment();

        return super.beforeToolCallback(tool, input, toolContext);
    }

    @Override
    public Maybe<Map<String, Object>> afterToolCallback(BaseTool tool, Map<String, Object> input, ToolContext toolContext, Map<String, Object> output) {
        Timer.Sample sample = toolSamples.remove(tool.name());
        if (sample != null) {
            sample.stop(Timer.builder("ai_agent_tool_call_duration_seconds")
                    .description("AI tool call duration")
                    .tag("tool", tool.name())
                    .tag("status", "success")
                    .register(meterRegistry));
        }

        return super.afterToolCallback(tool, input, toolContext, output);
    }
}
