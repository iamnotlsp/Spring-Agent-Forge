package com.lsp.trigger.http;

import com.lsp.api.response.Response;
import com.lsp.domain.credit.model.entity.CreditAccountEntity;
import com.lsp.domain.credit.model.entity.CreditGrantResultEntity;
import com.lsp.domain.credit.model.entity.CreditOrderEntity;
import com.lsp.domain.credit.model.entity.GroupBuyTeamSuccessEntity;
import com.lsp.domain.credit.model.valobj.enums.CreditOrderStatusEnumVO;
import com.lsp.domain.credit.service.ICreditService;
import com.lsp.types.enums.ResponseCode;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 额度服务 HTTP 入口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/credit/")
@CrossOrigin(origins = "*")
public class CreditServiceController {

    private final ICreditService creditService;

    public CreditServiceController(ICreditService creditService) {
        this.creditService = creditService;
    }

    @GetMapping("query_credit_order/{outTradeNo}")
    public Response<CreditOrderResponse> queryCreditOrder(@PathVariable("outTradeNo") String outTradeNo) {
        try {
            log.info("查询额度订单 outTradeNo:{}", outTradeNo);

            CreditOrderEntity creditOrderEntity = creditService.queryCreditOrder(outTradeNo);
            return success(buildCreditOrderResponse(creditOrderEntity));
        } catch (IllegalArgumentException e) {
            log.warn("查询额度订单参数非法 outTradeNo:{}", outTradeNo, e);
            return illegalParameter(e);
        } catch (Exception e) {
            log.error("查询额度订单失败 outTradeNo:{}", outTradeNo, e);
            return fail();
        }
    }

    @GetMapping("query_credit_order_list/{userId}")
    public Response<List<CreditOrderResponse>> queryCreditOrderList(@PathVariable("userId") String userId) {
        try {
            log.info("查询额度订单列表 userId:{}", userId);

            List<CreditOrderEntity> creditOrderEntityList = creditService.queryCreditOrderList(userId);
            return success(creditOrderEntityList.stream()
                    .map(this::buildCreditOrderResponse)
                    .collect(Collectors.toList()));
        } catch (IllegalArgumentException e) {
            log.warn("查询额度订单列表参数非法 userId:{}", userId, e);
            return illegalParameter(e);
        } catch (Exception e) {
            log.error("查询额度订单列表失败 userId:{}", userId, e);
            return fail();
        }
    }

    @GetMapping("query_credit_account/{userId}")
    public Response<CreditAccountResponse> queryCreditAccount(@PathVariable("userId") String userId) {
        try {
            log.info("查询额度账户 userId:{}", userId);

            CreditAccountEntity creditAccountEntity = creditService.queryCreditAccount(userId);
            return success(buildCreditAccountResponse(creditAccountEntity));
        } catch (IllegalArgumentException e) {
            log.warn("查询额度账户参数非法 userId:{}", userId, e);
            return illegalParameter(e);
        } catch (Exception e) {
            log.error("查询额度账户失败 userId:{}", userId, e);
            return fail();
        }
    }

    @PostMapping("create_credit_order")
    public Response<Boolean> createCreditOrder(@RequestBody CreateCreditOrderRequest request) {
        try {
            log.info("创建额度订单 userId:{} outTradeNo:{}", safeUserId(request), safeOutTradeNo(request));

            creditService.createCreditOrder(buildCreditOrderEntity(request));
            return success(true);
        } catch (IllegalArgumentException e) {
            log.warn("创建额度订单参数非法 request:{}", request, e);
            return illegalParameter(e);
        } catch (Exception e) {
            log.error("创建额度订单失败 request:{}", request, e);
            return fail();
        }
    }

    @PostMapping("purchase_credit_order")
    @Transactional(rollbackFor = Exception.class)
    public Response<CreditOrderResponse> purchaseCreditOrder(@RequestBody CreateCreditOrderRequest request) {
        try {
            log.info("普通购买额度 userId:{} outTradeNo:{}", safeUserId(request), safeOutTradeNo(request));

            CreditOrderEntity creditOrderEntity = creditService.purchaseCreditOrder(buildCreditOrderEntity(request));
            return success(buildCreditOrderResponse(creditOrderEntity));
        } catch (IllegalArgumentException e) {
            log.warn("普通购买额度参数非法 request:{}", request, e);
            return illegalParameter(e);
        } catch (Exception e) {
            log.error("普通购买额度失败 request:{}", request, e);
            return fail();
        }
    }

    @PostMapping("grant_group_buy_success")
    public Response<CreditGrantResultResponse> grantGroupBuySuccess(@RequestBody GroupBuyTeamSuccessRequest request) {
        try {
            log.info("拼团成团发放额度 teamId:{} outTradeNoList:{}", safeTeamId(request), safeOutTradeNoList(request));

            CreditGrantResultEntity result = creditService.grantCreditsByGroupBuySuccess(GroupBuyTeamSuccessEntity.builder()
                    .teamId(null == request ? null : request.getTeamId())
                    .outTradeNoList(null == request ? null : request.getOutTradeNoList())
                    .build());

            return success(buildCreditGrantResultResponse(result));
        } catch (IllegalArgumentException e) {
            log.warn("拼团成团发放额度参数非法 request:{}", request, e);
            return illegalParameter(e);
        } catch (Exception e) {
            log.error("拼团成团发放额度失败 request:{}", request, e);
            return fail();
        }
    }

    private CreditOrderEntity buildCreditOrderEntity(CreateCreditOrderRequest request) {
        if (null == request) {
            throw new IllegalArgumentException("request is required");
        }

        return CreditOrderEntity.builder()
                .userId(request.getUserId())
                .teamId(request.getTeamId())
                .orderId(request.getOrderId())
                .outTradeNo(request.getOutTradeNo())
                .goodsId(request.getGoodsId())
                .goodsName(request.getGoodsName())
                .credits(request.getCredits())
                .payPrice(request.getPayPrice())
                .status(CreditOrderStatusEnumVO.valueOfCode(request.getStatus()))
                .paidTime(request.getPaidTime())
                .build();
    }

    private CreditOrderResponse buildCreditOrderResponse(CreditOrderEntity creditOrderEntity) {
        if (null == creditOrderEntity) {
            return null;
        }

        CreditOrderStatusEnumVO status = creditOrderEntity.getStatus();
        return CreditOrderResponse.builder()
                .userId(creditOrderEntity.getUserId())
                .teamId(creditOrderEntity.getTeamId())
                .orderId(creditOrderEntity.getOrderId())
                .outTradeNo(creditOrderEntity.getOutTradeNo())
                .goodsId(creditOrderEntity.getGoodsId())
                .goodsName(creditOrderEntity.getGoodsName())
                .credits(creditOrderEntity.getCredits())
                .payPrice(creditOrderEntity.getPayPrice())
                .status(null == status ? null : status.getCode())
                .statusInfo(null == status ? null : status.getInfo())
                .paidTime(creditOrderEntity.getPaidTime())
                .grantTime(creditOrderEntity.getGrantTime())
                .createTime(creditOrderEntity.getCreateTime())
                .updateTime(creditOrderEntity.getUpdateTime())
                .targetCount(creditOrderEntity.getTargetCount())
                .joinedCount(creditOrderEntity.getJoinedCount())
                .teamStatus(creditOrderEntity.getTeamStatus())
                .build();
    }

    private CreditAccountResponse buildCreditAccountResponse(CreditAccountEntity creditAccountEntity) {
        if (null == creditAccountEntity) {
            return null;
        }

        return CreditAccountResponse.builder()
                .userId(creditAccountEntity.getUserId())
                .availableCredits(creditAccountEntity.getAvailableCredits())
                .frozenCredits(creditAccountEntity.getFrozenCredits())
                .totalGrantedCredits(creditAccountEntity.getTotalGrantedCredits())
                .totalUsedCredits(creditAccountEntity.getTotalUsedCredits())
                .status(creditAccountEntity.getStatus())
                .createTime(creditAccountEntity.getCreateTime())
                .updateTime(creditAccountEntity.getUpdateTime())
                .build();
    }

    private CreditGrantResultResponse buildCreditGrantResultResponse(CreditGrantResultEntity result) {
        if (null == result) {
            return null;
        }

        return CreditGrantResultResponse.builder()
                .teamId(result.getTeamId())
                .grantedOutTradeNoList(result.getGrantedOutTradeNoList())
                .skippedOutTradeNoList(result.getSkippedOutTradeNoList())
                .failedOutTradeNoList(result.getFailedOutTradeNoList())
                .build();
    }

    private <T> Response<T> success(T data) {
        return Response.<T>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(data)
                .build();
    }

    private <T> Response<T> illegalParameter(IllegalArgumentException e) {
        return Response.<T>builder()
                .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                .info(e.getMessage())
                .build();
    }

    private <T> Response<T> fail() {
        return Response.<T>builder()
                .code(ResponseCode.UN_ERROR.getCode())
                .info(ResponseCode.UN_ERROR.getInfo())
                .build();
    }

    private String safeUserId(CreateCreditOrderRequest request) {
        return null == request ? null : request.getUserId();
    }

    private String safeOutTradeNo(CreateCreditOrderRequest request) {
        return null == request ? null : request.getOutTradeNo();
    }

    private String safeTeamId(GroupBuyTeamSuccessRequest request) {
        return null == request ? null : request.getTeamId();
    }

    private List<String> safeOutTradeNoList(GroupBuyTeamSuccessRequest request) {
        return null == request ? null : request.getOutTradeNoList();
    }

    @Data
    public static class CreateCreditOrderRequest {
        private String userId;
        private String teamId;
        private String orderId;
        private String outTradeNo;
        private String goodsId;
        private String goodsName;
        private BigDecimal credits;
        private BigDecimal payPrice;
        private String status;
        private LocalDateTime paidTime;
    }

    @Data
    public static class GroupBuyTeamSuccessRequest {
        private String teamId;
        private List<String> outTradeNoList;
    }

    @Data
    @Builder
    public static class CreditOrderResponse {
        private String userId;
        private String teamId;
        private String orderId;
        private String outTradeNo;
        private String goodsId;
        private String goodsName;
        private BigDecimal credits;
        private BigDecimal payPrice;
        private String status;
        private String statusInfo;
        private LocalDateTime paidTime;
        private LocalDateTime grantTime;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
        private Integer targetCount;
        private Integer joinedCount;
        private Integer teamStatus;
    }

    @Data
    @Builder
    public static class CreditAccountResponse {
        private String userId;
        private BigDecimal availableCredits;
        private BigDecimal frozenCredits;
        private BigDecimal totalGrantedCredits;
        private BigDecimal totalUsedCredits;
        private String status;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }

    @Data
    @Builder
    public static class CreditGrantResultResponse {
        private String teamId;
        private List<String> grantedOutTradeNoList;
        private List<String> skippedOutTradeNoList;
        private List<String> failedOutTradeNoList;
    }

}
