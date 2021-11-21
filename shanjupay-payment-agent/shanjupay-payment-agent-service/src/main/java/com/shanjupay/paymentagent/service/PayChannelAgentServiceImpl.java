package com.shanjupay.paymentagent.service;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.paymentagent.api.PayChannelAgentService;
import com.shanjupay.paymentagent.api.conf.AliConfigParam;
import com.shanjupay.paymentagent.api.dto.AlipayBean;
import com.shanjupay.paymentagent.api.dto.PaymentResponseDTO;
import com.shanjupay.paymentagent.api.dto.TradeStatus;
import com.shanjupay.paymentagent.common.constant.AliCodeConstants;
import com.shanjupay.paymentagent.message.PayProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

@DubboService
@Slf4j
public class PayChannelAgentServiceImpl implements PayChannelAgentService {

    @Autowired
    PayProducer payProducer;

    @Override
    public PaymentResponseDTO createPayOrderByAliWAP(AliConfigParam aliConfigParam, AlipayBean alipayBean) throws BusinessException {

        //支付宝接口网关地址
        String url = aliConfigParam.getUrl();
        //支付宝应用Id
        String appId = aliConfigParam.getAppId();
        //应用私钥
        String rsaPrivateKey = aliConfigParam.getRsaPrivateKey();
        //json格式
        String format = aliConfigParam.getFormat();
        //字符集
        String charest = aliConfigParam.getCharest();
        //支付宝公钥
        String alipayPublicKey = aliConfigParam.getAlipayPublicKey();
        //签名类型
        String signtype = aliConfigParam.getSigntype();
        //支付成功跳转的url
        String returnUrl = aliConfigParam.getReturnUrl();
        //支付结果异步通知的url
        String notifyUrl = aliConfigParam.getNotifyUrl();

        //获得初始化的AlipayClient
        AlipayClient alipayClient = new DefaultAlipayClient(url, appId, rsaPrivateKey, format, charest, alipayPublicKey, signtype);
        //创建API对应的request
        AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();
        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        //商户订单，就是闪聚平台的订单
        model.setOutTradeNo(alipayBean.getOutTradeNo());
        //订单金额(元)
        model.setTotalAmount(alipayBean.getTotalAmount());
        //订单标题
        model.setSubject(alipayBean.getSubject());
        //订单描述
        model.setBody(alipayBean.getBody());
        //产品代码，固定
        model.setProductCode("QUICK_WAP_PAY");
        //订单过期时间
        model.setTimeoutExpress(alipayBean.getExpireTime());
        alipayRequest.setBizModel(model);
        alipayRequest.setReturnUrl(returnUrl);
        alipayRequest.setNotifyUrl(notifyUrl);
        String form="";
        try {
            //请求支付宝下单接口，发起http请求
            AlipayTradeWapPayResponse response = alipayClient.pageExecute(alipayRequest);
            PaymentResponseDTO paymentResponseDTO = new PaymentResponseDTO();
            log.info("调用支付宝下单接口，响应内容:{}", response.getBody());
            paymentResponseDTO.setContent(response.getBody());

            //向MQ发送一条延迟消息，支付结果查询
            PaymentResponseDTO<AliConfigParam> notice = new PaymentResponseDTO<>();
            //商户平台的订单号
            notice.setOutTradeNo(alipayBean.getOutTradeNo());
            notice.setContent(aliConfigParam);
            //标识是查询支付宝的接口
            notice.setMsg("ALIPAY_WAP");
            //发送消息
            payProducer.payOrderNotice(notice);

            return paymentResponseDTO;
        } catch (AlipayApiException e) {
            e.printStackTrace();
            throw new BusinessException(CommonErrorCode.E_400002);
        }
    }

    /**
     * 查询订单支付状态
     *
     * @param aliConfigParam 支付参数
     * @param outTradeNo     商户订单号
     * @return 支付结果
     * @throws BusinessException 自定义异常
     */
    @Override
    public PaymentResponseDTO queryPayOrderByAli(AliConfigParam aliConfigParam, String outTradeNo) throws BusinessException {
        //支付宝接口网关地址
        String url = aliConfigParam.getUrl();
        //支付宝应用Id
        String appId = aliConfigParam.getAppId();
        //应用私钥
        String rsaPrivateKey = aliConfigParam.getRsaPrivateKey();
        //json格式
        String format = aliConfigParam.getFormat();
        //字符集
        String charest = aliConfigParam.getCharest();
        //支付宝公钥
        String alipayPublicKey = aliConfigParam.getAlipayPublicKey();
        //签名类型
        String signtype = aliConfigParam.getSigntype();
        //支付成功跳转的url
        String returnUrl = aliConfigParam.getReturnUrl();
        //支付结果异步通知的url
        String notifyUrl = aliConfigParam.getNotifyUrl();

        AlipayClient alipayClient = new DefaultAlipayClient(url,appId,rsaPrivateKey,format, charest, alipayPublicKey, signtype);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setOutTradeNo(outTradeNo);
        request.setBizModel(model);

        AlipayTradeQueryResponse response = null;
        try {
            //请求支付宝订单状态查询接口
            response = alipayClient.execute(request);
            //支付宝响应的code，10000表示接口调用成功
            String code = response.getCode();

            if (AliCodeConstants.SUCCESSCODE.equals(code)){
                String tradeStatus = response.getTradeStatus();
                //解析支付宝返回的状态，解析成闪聚平台的TradeStatus
                TradeStatus status = convertAliTradeStatusToShanjuCode(tradeStatus);
                //String tradeNo(支付宝订单号), String outTradeNo(闪聚平台订单号), TradeStatus tradeState(订单状态), String msg
                return PaymentResponseDTO.success(response.getTradeNo(), response.getOutTradeNo(), status, response.getMsg());
            }

        } catch (AlipayApiException e) {

        }
        return PaymentResponseDTO.fail("支付宝订单状态查询失败", outTradeNo, TradeStatus.UNKNOWN);
    }

    //解析支付宝的订单状态为闪聚平台的状态
    private TradeStatus convertAliTradeStatusToShanjuCode(String aliTradeStatus){
        switch (aliTradeStatus){
            case AliCodeConstants.TRADE_FINISHED:
            case AliCodeConstants.TRADE_SUCCESS:
                return TradeStatus.SUCCESS;
            case AliCodeConstants.TRADE_CLOSED:
                return TradeStatus.REVOKED;
            case AliCodeConstants.WAIT_BUYER_PAY:
                return TradeStatus.USERPAYING;
            default:
                return TradeStatus.FAILED;
        }
    }
}
