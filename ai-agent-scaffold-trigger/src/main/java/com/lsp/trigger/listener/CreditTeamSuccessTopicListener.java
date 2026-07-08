package com.lsp.trigger.listener;

import com.alibaba.fastjson.JSON;
import com.lsp.domain.credit.model.entity.CreditGrantResultEntity;
import com.lsp.domain.credit.model.entity.GroupBuyTeamSuccessEntity;
import com.lsp.domain.credit.service.ICreditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 拼团成团消息监听器。
 */
@Slf4j
@Component
public class CreditTeamSuccessTopicListener {

    private final ICreditService creditService;

    public CreditTeamSuccessTopicListener(ICreditService creditService) {
        this.creditService = creditService;
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "${spring.rabbitmq.config.producer.topic_team_success.queue}", durable = "true"),
                    exchange = @Exchange(value = "${spring.rabbitmq.config.producer.exchange}", type = ExchangeTypes.TOPIC, durable = "true"),
                    key = "${spring.rabbitmq.config.producer.topic_team_success.routing_key}"
            )
    )
    public void listener(String message) {
        log.info("接收拼团成团消息 message:{}", message);

        GroupBuyTeamSuccessEntity groupBuyTeamSuccessEntity = JSON.parseObject(message, GroupBuyTeamSuccessEntity.class);
        CreditGrantResultEntity result = creditService.grantCreditsByGroupBuySuccess(groupBuyTeamSuccessEntity);

        log.info("拼团成团额度发放完成 result:{}", JSON.toJSONString(result));
        if (null != result && null != result.getFailedOutTradeNoList() && !result.getFailedOutTradeNoList().isEmpty()) {
            throw new IllegalStateException("拼团成团额度发放存在失败订单，等待 MQ 重试: " + JSON.toJSONString(result.getFailedOutTradeNoList()));
        }
    }

}
