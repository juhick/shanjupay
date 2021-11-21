package com.shanjupay.transaction.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.common.util.AmountUtil;
import com.shanjupay.common.util.EncryptUtil;
import com.shanjupay.common.util.PaymentUtil;
import com.shanjupay.merchant.api.AppService;
import com.shanjupay.merchant.api.MerchantService;
import com.shanjupay.paymentagent.api.PayChannelAgentService;
import com.shanjupay.paymentagent.api.conf.AliConfigParam;
import com.shanjupay.paymentagent.api.dto.AlipayBean;
import com.shanjupay.paymentagent.api.dto.PaymentResponseDTO;
import com.shanjupay.transaction.api.PayChannelService;
import com.shanjupay.transaction.api.TransactionService;
import com.shanjupay.transaction.api.dto.PayChannelParamDTO;
import com.shanjupay.transaction.api.dto.PayOrderDTO;
import com.shanjupay.transaction.api.dto.QRCodeDto;
import com.shanjupay.transaction.convert.PayOrderConvert;
import com.shanjupay.transaction.entity.PayOrder;
import com.shanjupay.transaction.mapper.PayOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@DubboService
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    @Value("${shanjupay.payurl}")
    String payUrl;

    @DubboReference
    AppService appService;

    @DubboReference
    MerchantService merchantService;

    @Autowired
    PayOrderMapper payOrderMapper;

    @DubboReference
    PayChannelAgentService payChannelAgentService;

    @Autowired
    PayChannelService payChannelService;

    @Override
    public String createStoreQRCode(QRCodeDto qrCodeDTO) throws BusinessException {
        //校验商户id和门店id和应用id的合法性
        //校验应用是否属于商户，校验门店是否属于商户
        verifyAppAndStore(qrCodeDTO.getMerchantId(), qrCodeDTO.getAppId(), qrCodeDTO.getStoreId());
        //组装url所需要的数据
        PayOrderDTO payOrderDTO = new PayOrderDTO();
        payOrderDTO.setMerchantId(qrCodeDTO.getMerchantId());
        payOrderDTO.setAppId(qrCodeDTO.getAppId());
        payOrderDTO.setStoreId(qrCodeDTO.getStoreId());
        payOrderDTO.setSubject(qrCodeDTO.getSubject());
        payOrderDTO.setBody(qrCodeDTO.getBody());
        payOrderDTO.setChannel("shanju_c2b");
        //转为json
        String jsonString = JSON.toJSONString(payOrderDTO);
        //base64进行编码
        String ticket = EncryptUtil.encodeUTF8StringBase64(jsonString);
        //目标是生成一个支付入口的url，需要携带参数将传入参数转为json，用base64编码
        String url = payUrl + ticket;
        return url;
    }

    @Override
    public PaymentResponseDTO submitOrderByAli(PayOrderDTO payOrderDTO) throws BusinessException {

        //保存订单到闪聚平台数据库
        //设置支付渠道
        payOrderDTO.setChannel("ALIPAY_WAP");
        //保存订单到闪聚平台数据库
        PayOrderDTO save = save(payOrderDTO);

        //调用支付渠道代理服务，调用支付宝下单接口
        PaymentResponseDTO paymentResponseDTO = alipayH5(save.getTradeNo());

        return paymentResponseDTO;
    }

    private PaymentResponseDTO alipayH5(String tradeNo){
        //订单信息，从数据库查询
        PayOrderDTO payOrderDTO = queryPayOrder(tradeNo);
        //组装alipayBean
        AlipayBean alipayBean = new AlipayBean();
        alipayBean.setOutTradeNo(payOrderDTO.getTradeNo());
        try {
            alipayBean.setTotalAmount(AmountUtil.changeF2Y(payOrderDTO.getTotalAmount().toString()));
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(CommonErrorCode.E_300006);
        }
        alipayBean.setSubject(payOrderDTO.getSubject());
        alipayBean.setBody(payOrderDTO.getBody());
        alipayBean.setExpireTime("30m");

        //支付渠道配置参数，从数据库查
        PayChannelParamDTO payChannelParamDTO = payChannelService.queryParamByAppPlatformAndPayChannel(payOrderDTO.getAppId(), "shanju_c2b", "ALIPAY_WAP");
        String paramJson = payChannelParamDTO.getParam();
        //支付渠道参数
        AliConfigParam aliConfigParam = JSON.parseObject(paramJson, AliConfigParam.class);
        //字符编码
        aliConfigParam.setCharest("utf-8");

        PaymentResponseDTO paymentResponseDTO = payChannelAgentService.createPayOrderByAliWAP(aliConfigParam, alipayBean);
        return paymentResponseDTO;
    }

    @Override
    public PayOrderDTO queryPayOrder(String tradeNo){
        PayOrder payOrder = payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrder>().eq(PayOrder::getTradeNo, tradeNo));
        return PayOrderConvert.INSTANCE.entity2dto(payOrder);
    }

    /**
     * 更新订单状态
     *
     * @param tradeNo           闪聚平台订单号
     * @param payChannelTradeNo 支付宝或微信的交易流水号（第三方支付系统的订单号）
     * @param state             订单状态、交易状态或支付状态：0-订单生产，1-支付中（目前未使用），2-支付成功，4-关闭，5-失败
     * @throws BusinessException 自定义异常
     */
    @Override
    public void updateOrderTradeNoAndTradeState(String tradeNo, String payChannelTradeNo, String state) throws BusinessException {
        LambdaUpdateWrapper<PayOrder> payOrderLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        payOrderLambdaUpdateWrapper.eq(PayOrder::getTradeNo, tradeNo)
                        .set(PayOrder::getTradeState, state)
                        .set(PayOrder::getPayChannelTradeNo, payChannelTradeNo);
        if (state != null && state.equals("2")){
            payOrderLambdaUpdateWrapper.set(PayOrder::getPaySuccessTime, LocalDateTime.now());
        }
        payOrderMapper.update(null, payOrderLambdaUpdateWrapper);
    }


    //保存订单（公用）
    private PayOrderDTO save(PayOrderDTO payOrderDTO) throws BusinessException {
        PayOrder entity = PayOrderConvert.INSTANCE.dto2entity(payOrderDTO);
        //采用雪花片算法生成订单号
        entity.setTradeNo(PaymentUtil.genUniquePayOrderNo());
        //创建时间
        entity.setCreateTime(LocalDateTime.now());
        //过期时间是30分钟后
        entity.setExpireTime(LocalDateTime.now().plus(30, ChronoUnit.MINUTES));
        //设置货币类型为人民币
        entity.setCurrency("CNY");
        //设置订单状态为订单生成
        entity.setTradeState("0");
        //插入订单
        payOrderMapper.insert(entity);
        return PayOrderConvert.INSTANCE.entity2dto(entity);
    }

    private Boolean verifyAppAndStore(Long merchantId, String appId, Long storeId){
        Boolean aBoolean = appService.queryAppInMerchant(appId, merchantId);
        if (!aBoolean){
            throw new BusinessException(CommonErrorCode.E_200005);
        }

        Boolean bBoolean = merchantService.queryStoreInMerchant(storeId, merchantId);
        if (!bBoolean){
            throw new BusinessException(CommonErrorCode.E_200006);
        }

        return true;
    }
}
