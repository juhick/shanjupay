package com.shanjupay.transaction.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shanjupay.common.cache.Cache;
import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.common.util.RedisUtil;
import com.shanjupay.transaction.api.PayChannelService;
import com.shanjupay.transaction.api.dto.PayChannelDTO;
import com.shanjupay.transaction.api.dto.PayChannelParamDTO;
import com.shanjupay.transaction.api.dto.PlatformChannelDTO;
import com.shanjupay.transaction.convert.PayChannelParamConvert;
import com.shanjupay.transaction.convert.PlatformChannelConvert;
import com.shanjupay.transaction.entity.AppPlatformChannel;
import com.shanjupay.transaction.entity.PayChannelParam;
import com.shanjupay.transaction.entity.PlatformChannel;
import com.shanjupay.transaction.mapper.AppPlatformChannelMapper;
import com.shanjupay.transaction.mapper.PayChannelParamMapper;
import com.shanjupay.transaction.mapper.PlatformChannelMapper;
import com.shanjupay.transaction.mapper.PlatformPayChannelMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@DubboService
public class PayChannelServiceImpl implements PayChannelService {

    @Autowired
    Cache cache;

    @Autowired
    PlatformChannelMapper platformChannelMapper;

    @Autowired
    AppPlatformChannelMapper appPlatformChannelMapper;

    @Autowired
    PlatformPayChannelMapper platformPayChannelMapper;

    @Autowired
    PayChannelParamMapper payChannelParamMapper;

    @Override
    public List<PlatformChannelDTO> queryPlatformChannel() throws BusinessException {

        //查询platform_channel表中的全部记录
        List<PlatformChannel> platformChannels = platformChannelMapper.selectList(null);

        return PlatformChannelConvert.INSTANCE.listentity2listdto(platformChannels);
    }

    @Override
    public void bindPlatformChannelForApp(String appId, String platformChannelCodes) throws BusinessException {

        //根据应用id和服务类型code查询，如果已经绑定则不再插入，否则插入记录
        AppPlatformChannel appPlatformChannel = appPlatformChannelMapper.selectOne(new LambdaQueryWrapper<AppPlatformChannel>().eq(AppPlatformChannel::getAppId, appId).eq(AppPlatformChannel::getPlatformChannel, platformChannelCodes));
        if (appPlatformChannel == null){
            //向app_platform_channel表插入记录
            AppPlatformChannel entity = new AppPlatformChannel();
            entity.setAppId(appId); //应用Id
            entity.setPlatformChannel(platformChannelCodes); //服务类型Code
            appPlatformChannelMapper.insert(entity);
        }


    }

    @Override
    public int queryAppBindPlatformChannel(String appId, String platformChannel) throws BusinessException {
        AppPlatformChannel appPlatformChannel = appPlatformChannelMapper.selectOne(new LambdaQueryWrapper<AppPlatformChannel>().eq(AppPlatformChannel::getAppId, appId).eq(AppPlatformChannel::getPlatformChannel, platformChannel));
        if (appPlatformChannel != null){
            return 1;
        }
        return 0;
    }

    @Override
    public List<PayChannelDTO> queryPayChannelByPlatformChannel(String platformChannelCode) throws BusinessException {
        //调用mapper查询数据库platform_pay_channel， pay_channel， platform_channel
        return platformPayChannelMapper.selectPayChannelByPlatformChannel(platformChannelCode);
    }

    @Override
    public void savePayChannelParam(PayChannelParamDTO payChannelParamDTO) throws BusinessException {
        if (payChannelParamDTO == null || payChannelParamDTO.getChannelName() == null || payChannelParamDTO.getParam() == null){
            throw new BusinessException(CommonErrorCode.E_300009);
        }
        //根据应用、服务类型、支付渠道查询一条记录
        //根据应用和服务类型查询应用与服务的绑定id
        Long appPlatformChannelId = selectIdByAppPlatformChannel(payChannelParamDTO.getAppId(), payChannelParamDTO.getPlatformChannelCode());
        if (appPlatformChannelId == null){
            throw new BusinessException(CommonErrorCode.E_300010);
        }

        //根据应用与服务类型的绑定id查询PayChannelParam的一条记录
        PayChannelParam entity = payChannelParamMapper.selectOne(new LambdaQueryWrapper<PayChannelParam>().eq(PayChannelParam::getAppPlatformChannelId, appPlatformChannelId).eq(PayChannelParam::getPayChannel, payChannelParamDTO.getPayChannel()));
        //如果存在配置则更新
        if (entity != null){
            entity.setChannelName(payChannelParamDTO.getChannelName());//配置名称
            entity.setParam(payChannelParamDTO.getParam());//json格式的配置参数
            payChannelParamMapper.updateById(entity);
        } else {
            //否则添加配置
            PayChannelParam entityNew = PayChannelParamConvert.INSTANCE.dto2entity(payChannelParamDTO);
            entityNew.setId(null);
            entityNew.setAppPlatformChannelId(appPlatformChannelId);
            payChannelParamMapper.insert(entityNew);
        }

        //向redis保存支付渠道参数
        updateCache(payChannelParamDTO.getAppId(), payChannelParamDTO.getPlatformChannelCode());
    }

    /**
     * 根据应用和服务类型将查询到的支付渠道参数列表写入redis
     * @param appId 应用Id
     * @param platformChannelCode 支付渠道代码
     */
    private void updateCache(String appId, String platformChannelCode){
        //得到redis中的key（支付渠道参数配置列表的key）
        //格式：SJ_PAY_PARAM:应用Id:服务类型
        String redisKey = RedisUtil.keyBuilder(appId, platformChannelCode);
        //根据key查询redis
        Boolean exists = cache.exists(redisKey);
        if (exists){
            cache.del(redisKey);
        }
        //根据应用id和服务类型code查询支付渠道列表，将支付渠道参数列表写入redis
        Long appPlatformChannelId = selectIdByAppPlatformChannel(appId, platformChannelCode);
        if (appPlatformChannelId != null){
            List<PayChannelParam> payChannelParams = payChannelParamMapper.selectList(new LambdaQueryWrapper<PayChannelParam>().eq(PayChannelParam::getAppPlatformChannelId, appPlatformChannelId));
            List<PayChannelParamDTO> payChannelParamDTOS = PayChannelParamConvert.INSTANCE.listentity2listdto(payChannelParams);
            cache.set(redisKey, JSON.toJSON(payChannelParamDTOS).toString());
        }
    }

    @Override
    public List<PayChannelParamDTO> queryPayChannelParamByAppAndPlatform(String appId, String platformChannel) throws BusinessException {

        //TODO 先从redis查询，若查询到则直接返回，否则从数据库查询，从数据库查询完毕后再保存到redis
        String redisKey = RedisUtil.keyBuilder(appId, platformChannel);
        Boolean exists = cache.exists(redisKey);
        if (exists){
            //从redis获取支付渠道参数列表（json串）
            String PayChannelParamDTO_String = cache.get(redisKey);
            //将json串转成List<PayChannelParamDTO>
            List<PayChannelParamDTO> payChannelParamDTOS = JSON.parseArray(PayChannelParamDTO_String, PayChannelParamDTO.class);
            return payChannelParamDTOS;
        }

        //根据应用和服务类型找到他们的绑定id
        Long appPlatformChannelId = selectIdByAppPlatformChannel(appId, platformChannel);
        if (appPlatformChannelId == null){
            return null;
        }

        //根据应用和服务类型绑定id查询支付渠道参数配置记录
        List<PayChannelParam> payChannelParams = payChannelParamMapper.selectList(new LambdaQueryWrapper<PayChannelParam>().eq(PayChannelParam::getAppPlatformChannelId, appPlatformChannelId));
        List<PayChannelParamDTO> payChannelParamDTOS = PayChannelParamConvert.INSTANCE.listentity2listdto(payChannelParams);
        updateCache(appId, platformChannel);
        return payChannelParamDTOS;

    }

    @Override
    public PayChannelParamDTO queryParamByAppPlatformAndPayChannel(String appId, String platformChannel, String payChannel) throws BusinessException {
        List<PayChannelParamDTO> payChannelParamDTOS = queryPayChannelParamByAppAndPlatform(appId, platformChannel);
        for (PayChannelParamDTO payChannelParamDTO:payChannelParamDTOS){
            if (payChannelParamDTO.getPayChannel().equals(payChannel)){
                return payChannelParamDTO;
            }
        }
        return null;
    }

    private Long selectIdByAppPlatformChannel(String appId, String platformChannelCode){
        AppPlatformChannel appPlatformChannel = appPlatformChannelMapper.selectOne(new LambdaQueryWrapper<AppPlatformChannel>().eq(AppPlatformChannel::getAppId, appId).eq(AppPlatformChannel::getPlatformChannel, platformChannelCode));
        if (appPlatformChannel != null){
            //应用与服务类型的绑定Id
            return appPlatformChannel.getId();
        }
        return null;
    }
}
