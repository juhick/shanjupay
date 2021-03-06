package com.shanjupay.transaction.api;

import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.paymentagent.api.dto.PaymentResponseDTO;
import com.shanjupay.transaction.api.dto.PayOrderDTO;
import com.shanjupay.transaction.api.dto.QRCodeDto;

import java.util.Map;

/**
 * 交易相关的服务接口
 * Created by Administrator.
 * @author gaoruan
 */
public interface TransactionService {

    /**
     * 生成门店二维码的url
     * @param qrCodeDto 传入merchantId,appId、storeId、channel、subject、body
     * @return 支付入口（url），要携带参数（将传入的参数转成json，用base64编码）
     * @throws BusinessException 猪肚包发图图插板阀
     */
    String createStoreQRCode(QRCodeDto qrCodeDto)throws BusinessException;

    /**
     * 保存支付宝订单，1、保存订单到闪聚平台，2、调用支付渠道代理服务调用支付宝的接口
     * @param payOrderDTO 订单信息
     * @return 支付结果
     * @throws BusinessException 自定义异常
     */
    PaymentResponseDTO submitOrderByAli(PayOrderDTO payOrderDTO) throws BusinessException;

    /**
     * 根据订单号查询订单信息
     * @param tradeNo 订单号
     * @return 查询到的订单信息
     */
    PayOrderDTO queryPayOrder(String tradeNo);

    /**
     * 更新订单支付状态
     *
     * @param tradeNo           闪聚平台订单号
     * @param payChannelTradeNo 支付宝或微信的交易流水号(第三方支付系统的订单)
     * @param state             订单状态  交易状态支付状态,0-订单生成,1-支付中(目前未使用),2-支付成功,4-关闭 5--失败
     * @throws BusinessException 自定义异常
     */
    void updateOrderTradeNoAndTradeState(String tradeNo, String payChannelTradeNo, String state) throws BusinessException;
}
