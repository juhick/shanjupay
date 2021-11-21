package com.shanjupay.transaction.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

@ApiModel(value = "OrderConfirmVO", description = "订单确认信息")
@Data
public class OrderConfirmVO {

    //应用Id
    private String appId;
    //交易单号
    private String tradeNo;
    //微信openId
    private String openId;
    //门店Id
    private String storeId;
    //服务类型
    private String channel;
    //订单类型
    private String body;
    //订单信息
    private String subject;
    //金额
    private String totalAmount;
}
