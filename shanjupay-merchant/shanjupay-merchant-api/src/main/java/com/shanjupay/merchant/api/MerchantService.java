package com.shanjupay.merchant.api;

import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.PageVO;
import com.shanjupay.merchant.api.dto.MerchantDTO;
import com.shanjupay.merchant.api.dto.MerchantQueryDTO;
import com.shanjupay.merchant.api.dto.StaffDTO;
import com.shanjupay.merchant.api.dto.StoreDTO;

import java.util.List;

/**
 * Created by Administrator.
 * @author gaoruan
 */
public interface MerchantService {

	/**
	 * 根据Id查询商户
	 * @param id 商户Id
	 * @return 查询到的商户信息
	 */
	MerchantDTO queryMerchantById(Long id);

    /**
     * 根据租户id查询商户的信息
     * @param tenantId 租户Id
     * @return 查询到的商户信息
     */
    MerchantDTO queryMerchantByTenantId(Long tenantId);

    /**
     *  注册商户服务接口，接收账号、密码、手机号，为了可扩展性使用merchantDto接收数据
     * @param merchantDTO 商户注册信息
     * @return 注册成功的商户信息
	 * @throws BusinessException 自定义异常
     */
    MerchantDTO createMerchant(MerchantDTO merchantDTO) throws BusinessException;

    /**
     * 资质申请接口
     * @param merchantId 商户id
     * @param merchantDTO 资质申请的信息
     * @throws BusinessException 自定义异常
     */
    void applyMerchant(Long merchantId,MerchantDTO merchantDTO) throws BusinessException;

    /**
     * 新增门店
     * @param storeDTO 门店信息
     * @return 新增成功的门店信息
     * @throws BusinessException 自定义异常
     */
    StoreDTO createStore(StoreDTO storeDTO) throws BusinessException;


    /**
     * 新增员工
     * @param staffDTO 员工信息
     * @return 新增成功的员工信息
     * @throws BusinessException 自定义异常
     */
    StaffDTO createStaff(StaffDTO staffDTO) throws BusinessException;


    /**
     * 将员工设置为门店的管理员
     * @param storeId 商户Id
     * @param staffId 员工Id
     * @throws BusinessException 自定义异常
     */
    void bindStaffToStore(Long storeId, Long staffId) throws BusinessException;

    /**
     * 门店列表的查询
     * @param storeDTO 查询条件，必要参数：商户id
     * @param pageNo  页码
     * @param pageSize 分页记录数
     * @return 查询到的信息分页
     */
    PageVO<StoreDTO> queryStoreByPage(StoreDTO storeDTO,Integer pageNo,Integer pageSize);

    /**
     * 校验门店是否属于商户
     * @param storeId 门店Id
     * @param merchantId 商户Id
     * @return 门店是否属于商户
     */
    Boolean queryStoreInMerchant(Long storeId, Long merchantId);

	/**
	 * 商户分页条件查询
	 * @param merchantQueryDTO 商户请求信息
	 * @param pageNo 页号
	 * @param pageSize 页面大小
	 * @return 查询到的页面信息
	 * @throws BusinessException 自定义异常
	 */
	PageVO<MerchantDTO> queryMerchantPage(MerchantQueryDTO merchantQueryDTO, Integer pageNo, Integer pageSize) throws BusinessException;

	/**
	 * 商户资质审核
	 * @param merchantId 商户资质
	 * @param auditStatus 资质状态
	 * @throws BusinessException 自定义异常
	 */
	void verifyMerchant(Long merchantId, String auditStatus) throws BusinessException;

	/**
	 * 商户下新增门店,并设置管理员
	 * @param storeDTO 门店信息
	 */
	StoreDTO createStore(StoreDTO storeDTO, List<Long> staffIds) throws BusinessException;

	/**
	 * 分页查询商户下的员工
	 * @param staffDTO 门店信息
	 * @param pageNo 页号
	 * @param pageSize 页面大小
	 * @return 查询到的页面信息
	 * @throws BusinessException 自定义异常
	 */
	PageVO<StaffDTO> queryStaffByPage(StaffDTO staffDTO, Integer pageNo, Integer pageSize)
			throws BusinessException;

	/**
	 * 查询某个门店
	 * @param id 门店Id
	 * @return 查询到的门店信息
	 * @throws BusinessException 自定义异常
	 */
	StoreDTO queryStoreById(Long id) throws BusinessException;

	/**
	 * 修改门店
	 * @param store 门店信息
	 * @param staffIds 员工列表
	 * @throws BusinessException 自定义异常
	 */
	void modifyStore(StoreDTO store, List<Long> staffIds) throws BusinessException;

	/**
	 * 删除某门店
	 * @param id 门店Id
	 * @throws BusinessException 自定义异常
	 */
	void removeStore(Long id) throws BusinessException;

	/**
	 * 商户新增员工和账号
	 * @param staffDTO 员工信息
	 * @param roleCodes 角色代码
	 * @throws BusinessException 自定义异常
	 */
	void createStaffAndAccount(StaffDTO staffDTO, String [] roleCodes) throws BusinessException;

	/**
	 * 查询员工详情
	 * @param id 员工Id
	 * @param tenantId 租户Id
	 * @return 查询到的员工信息
	 */
	StaffDTO queryStaffDetail(Long id, Long tenantId);

	/**
	 * 修改员工信息
	 * @param staff 员工信息
	 * @param roleCodes 角色代码
	 * @throws BusinessException 自定义异常
	 */
	void modifyStaff(StaffDTO staff, String [] roleCodes) throws BusinessException;

	/**
	 * 删除员工
	 * @param id 员工Id
	 * @throws BusinessException 自定义异常
	 */
	void removeStaff(Long id) throws BusinessException;
}
