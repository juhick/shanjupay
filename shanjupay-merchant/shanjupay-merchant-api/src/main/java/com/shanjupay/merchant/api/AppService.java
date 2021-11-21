package com.shanjupay.merchant.api;

import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.merchant.api.dto.AppDTO;

import java.util.List;

/**
 * 应用管理相关的接口
 * Created by Administrator.
 * @author gaoruan
 */
public interface AppService {

    /**
     * 创建应用
     * @param merchantId 商户id
     * @param appDTO 应用信息
     * @return 创建成功的应用信息
     * @throws BusinessException 自定义异常
     */
    AppDTO createApp(Long merchantId,AppDTO appDTO) throws BusinessException;

    /**
     * 根据商户id查询应用列表
     * @param merchantId 商户Id
     * @return 查询到的应用列表
     * @throws BusinessException 自定义异常
     */
    List<AppDTO> queryAppByMerchant(Long merchantId) throws BusinessException;

    /**
     * 根据应用id查询应用信息
     * @param appId 应用Id
     * @return 查询到的应用信息
     * @throws BusinessException 自定义异常
     */
    AppDTO getAppById(String appId)throws BusinessException;


    /**
     * 校验应用是否属于商户
     * @param appId 应用Id
     * @param merchantId 商户Id
     * @return 应用是否属于租户
     */
    Boolean queryAppInMerchant(String appId,Long merchantId);

}
