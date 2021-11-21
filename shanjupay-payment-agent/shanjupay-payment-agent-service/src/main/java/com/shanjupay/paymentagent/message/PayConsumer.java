package com.shanjupay.paymentagent.message;

import com.alibaba.fastjson.JSON;
import com.shanjupay.paymentagent.api.PayChannelAgentService;
import com.shanjupay.paymentagent.api.conf.AliConfigParam;
import com.shanjupay.paymentagent.api.dto.PaymentResponseDTO;
import com.shanjupay.paymentagent.api.dto.TradeStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(topic = "TP_PAYMENT_ORDER", consumerGroup = "CID_PAYMENT_CONSUMER")
@Slf4j
public class PayConsumer implements RocketMQListener<MessageExt> {

    @Autowired
    PayChannelAgentService payChannelAgentService;

    @Autowired
    PayProducer payProducer;

    @Override
    public void onMessage(MessageExt messageExt) {
        byte[] body = messageExt.getBody();
        String jsonString = new String(body);
        log.info("支付代理服务接收到查询订单的消息:{}", JSON.toJSONString(jsonString));
        //将消息转成对象
        PaymentResponseDTO paymentResponseDTO = JSON.parseObject(jsonString, PaymentResponseDTO.class);
        String outTradeNo = paymentResponseDTO.getOutTradeNo();
        String params = String.valueOf(paymentResponseDTO.getContent());
        //转成对象
        AliConfigParam aliConfigParam = JSON.parseObject(params, AliConfigParam.class);

        PaymentResponseDTO responseDTO = null;

        if ("ALIPAY_WAP".equals(paymentResponseDTO.getMsg())){
            //调用支付宝订单状态查询接口
            responseDTO = payChannelAgentService.queryPayOrderByAli(aliConfigParam, outTradeNo);
        }else if ("WX_JSAPI".equals(paymentResponseDTO.getMsg())){
            //TODO 调用微信的接口查询
        }

        //当没有获取到订单结果时，抛出异常，再次重试消费
        if (responseDTO == null || TradeStatus.UNKNOWN.equals(responseDTO.getTradeState()) || TradeStatus.USERPAYING.equals(responseDTO.getTradeState())){
            throw new RuntimeException("支付状态未知，等待重试");
        }

        //将订单状态再次发到mq
        payProducer.payResultNotice(responseDTO);
    }
}
