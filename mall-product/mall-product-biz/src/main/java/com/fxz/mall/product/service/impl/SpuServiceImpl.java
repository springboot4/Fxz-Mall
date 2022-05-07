package com.fxz.mall.product.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fxz.mall.product.dto.AttributeValueDto;
import com.fxz.mall.product.dto.GoodsDto;
import com.fxz.mall.product.dto.SkuDto;
import com.fxz.mall.product.dto.SpuDto;
import com.fxz.mall.product.entity.Sku;
import com.fxz.mall.product.entity.SkuAttributeValue;
import com.fxz.mall.product.entity.Spu;
import com.fxz.mall.product.entity.SpuAttributeValue;
import com.fxz.mall.product.enums.AttributeTypeEnum;
import com.fxz.mall.product.mapper.SpuMapper;
import com.fxz.mall.product.service.SpuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品表
 *
 * @author fxz
 * @date 2022-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpuServiceImpl extends ServiceImpl<SpuMapper, Spu> implements SpuService {

	private final SpuMapper spuMapper;

	private final SkuServiceImpl skuService;

	private final SpuAttributeValueServiceImpl spuAttributeValueService;

	private final SkuAttributeValueServiceImpl skuAttributeValueService;

	/**
	 * 添加
	 */
	@Override
	public Boolean addSpu(SpuDto spuDto) {
		Spu spu = new Spu();
		BeanUtils.copyProperties(spuDto, spu);
		spuMapper.insert(spu);
		return Boolean.TRUE;
	}

	/**
	 * 修改
	 */
	@Override
	public Boolean updateSpu(SpuDto spuDto) {
		Spu spu = new Spu();
		BeanUtils.copyProperties(spuDto, spu);
		spuMapper.updateById(spu);
		return Boolean.TRUE;
	}

	/**
	 * 分页
	 */
	@Override
	public IPage<Spu> pageSpu(Page<Spu> pageParam, Spu spu) {
		return spuMapper.selectPage(pageParam, Wrappers.emptyWrapper());
	}

	/**
	 * 获取单条
	 */
	@Override
	public Spu findById(Long id) {
		return spuMapper.selectById(id);
	}

	/**
	 * 获取全部
	 */
	@Override
	public List<Spu> findAll() {
		return spuMapper.selectList(Wrappers.emptyWrapper());
	}

	/**
	 * 删除
	 */
	@Override
	public Boolean deleteSpu(Long id) {
		spuMapper.deleteById(id);
		return Boolean.TRUE;
	}

	/**
	 * 保存商品
	 * @param goodsDto 商品信息
	 * @return 是否保存成功
	 */
	@Override
	public Boolean addGoods(GoodsDto goodsDto) {
		// 保存spu信息
		Long goodsId = this.saveSpu(goodsDto);

		// 属性保存
		List<AttributeValueDto> attrValList = goodsDto.getAttrList();
		this.saveAttribute(goodsId, attrValList);

		// sku保存
		List<SkuDto> skuList = goodsDto.getSkuList();
		return this.saveSku(goodsId, skuList);
	}

	/**
	 * 保存Sku信息
	 * @param goodsId spuId
	 * @param skuList sku列表
	 * @return 是否保存成功
	 */
	private Boolean saveSku(Long goodsId, List<SkuDto> skuList) {
		// 新增/修改SKU
		skuList.forEach(skuDto -> {
			Sku sku = new Sku();
			BeanUtils.copyProperties(skuDto, sku);
			sku.setSpuId(goodsId);

			// 保存sku信息
			skuService.save(sku);

			// 保存sku属性
			List<AttributeValueDto> specValList = skuDto.getSpecValList();
			if (CollectionUtils.isNotEmpty(specValList)) {
				List<SkuAttributeValue> skuAttributeValues = specValList.stream().map(item -> {
					SkuAttributeValue skuAttributeValue = new SkuAttributeValue();
					BeanUtils.copyProperties(item, skuAttributeValue);
					skuAttributeValue.setSpuId(goodsId);
					skuAttributeValue.setSkuId(sku.getId());
					return skuAttributeValue;
				}).collect(Collectors.toList());
				skuAttributeValueService.saveOrUpdateBatch(skuAttributeValues);
			}

		});

		return Boolean.TRUE;
	}

	/**
	 * 保存spu的属性
	 * @param goodsId spuId
	 * @param attrValList 属性列表
	 */
	private void saveAttribute(Long goodsId, List<AttributeValueDto> attrValList) {
		List<SpuAttributeValue> spuAttributeValueList = attrValList.stream().map(item -> {
			SpuAttributeValue spuAttributeValue = new SpuAttributeValue();
			BeanUtils.copyProperties(item, spuAttributeValue);
			spuAttributeValue.setSpuId(goodsId);
			spuAttributeValue.setType(AttributeTypeEnum.ATTRIBUTE.getValue());
			return spuAttributeValue;
		}).collect(Collectors.toList());
		if (CollectionUtils.isNotEmpty(spuAttributeValueList)) {
			spuAttributeValueService.saveOrUpdateBatch(spuAttributeValueList);
		}
	}

	private Long saveSpu(GoodsDto goodsDto) {
		Spu spu = new Spu();
		BeanUtil.copyProperties(goodsDto, spu);
		// 商品图册
		spu.setAlbum(JSONUtil.toJsonStr(goodsDto.getSubPicUrls()));
		boolean result = this.saveOrUpdate(spu);
		return result ? spu.getId() : 0;
	}

}