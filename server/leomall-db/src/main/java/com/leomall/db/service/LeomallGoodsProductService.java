package com.leomall.db.service;

import org.apache.ibatis.annotations.Param;
import com.leomall.db.dao.GoodsProductMapper;
import com.leomall.db.dao.LeomallGoodsProductMapper;
import com.leomall.db.domain.LeomallGoodsProduct;
import com.leomall.db.domain.LeomallGoodsProductExample;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeomallGoodsProductService {
    @Resource
    private LeomallGoodsProductMapper leomallGoodsProductMapper;
    @Resource
    private GoodsProductMapper goodsProductMapper;

    public List<LeomallGoodsProduct> queryByGid(Integer gid) {
        LeomallGoodsProductExample example = new LeomallGoodsProductExample();
        example.or().andGoodsIdEqualTo(gid).andDeletedEqualTo(false);
        return leomallGoodsProductMapper.selectByExample(example);
    }

    public LeomallGoodsProduct findById(Integer id) {
        return leomallGoodsProductMapper.selectByPrimaryKey(id);
    }

    public void deleteById(Integer id) {
        leomallGoodsProductMapper.logicalDeleteByPrimaryKey(id);
    }

    public void add(LeomallGoodsProduct goodsProduct) {
        goodsProduct.setAddTime(LocalDateTime.now());
        goodsProduct.setUpdateTime(LocalDateTime.now());
        leomallGoodsProductMapper.insertSelective(goodsProduct);
    }

    public int count() {
        LeomallGoodsProductExample example = new LeomallGoodsProductExample();
        example.or().andDeletedEqualTo(false);
        return (int) leomallGoodsProductMapper.countByExample(example);
    }

    public void deleteByGid(Integer gid) {
        LeomallGoodsProductExample example = new LeomallGoodsProductExample();
        example.or().andGoodsIdEqualTo(gid);
        leomallGoodsProductMapper.logicalDeleteByExample(example);
    }

    public int addStock(Integer id, Short num){
        return goodsProductMapper.addStock(id, num);
    }

    public int reduceStock(Integer id, Short num){
        return goodsProductMapper.reduceStock(id, num);
    }

    public void updateById(LeomallGoodsProduct product) {
        product.setUpdateTime(LocalDateTime.now());
        leomallGoodsProductMapper.updateByPrimaryKeySelective(product);
    }
}