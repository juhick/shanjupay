package com.shanjupay.paymentagent.api;

import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.paymentagent.api.conf.AliConfigParam;
import com.shanjupay.paymentagent.api.dto.AlipayBean;
import com.shanjupay.paymentagent.api.dto.PaymentResponseDTO;

public interface PayChannelAgentService {

    /**
     * 创建支付宝网页支付所需的订单
     * @param aliConfigParam 支付渠道配置的参数（配置的支付宝的必要参数）
     * @param alipayBean 业务参数（商户订单号，订单标题，订单描述）
     * @return 统一返回PaymentResponseDTO
     * @throws BusinessException 自定义异常
     */
    PaymentResponseDTO createPayOrderByAliWAP(AliConfigParam aliConfigParam, AlipayBean alipayBean) throws BusinessException;

    /**
     * 查询订单支付状态
     * @param aliConfigParam 支付参数
     * @param outTradeNo 商户订单号
     * @return 支付结果
     * @throws BusinessException 自定义异常
     */
    PaymentResponseDTO queryPayOrderByAli(AliConfigParam aliConfigParam, String outTradeNo) throws BusinessException;
}
