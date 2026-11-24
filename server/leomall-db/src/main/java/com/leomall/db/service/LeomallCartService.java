package com.leomall.db.service;

import com.github.pagehelper.PageHelper;
import com.leomall.db.dao.LeomallCartMapper;
import com.leomall.db.domain.LeomallCart;
import com.leomall.db.domain.LeomallCartExample;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeomallCartService {
    @Resource
    private LeomallCartMapper cartMapper;

    public LeomallCart queryExist(Integer goodsId, Integer productId, Integer userId) {
        LeomallCartExample example = new LeomallCartExample();
        example.or().andGoodsIdEqualTo(goodsId).andProductIdEqualTo(productId).andUserIdEqualTo(userId).andDeletedEqualTo(false);
        return cartMapper.selectOneByExample(example);
    }

    public void add(LeomallCart cart) {
        cart.setAddTime(LocalDateTime.now());
        cart.setUpdateTime(LocalDateTime.now());
        cartMapper.insertSelective(cart);
    }

    public int updateById(LeomallCart cart) {
        cart.setUpdateTime(LocalDateTime.now());
        return cartMapper.updateByPrimaryKeySelective(cart);
    }

    public List<LeomallCart> queryByUid(int userId) {
        LeomallCartExample example = new LeomallCartExample();
        example.or().andUserIdEqualTo(userId).andDeletedEqualTo(false);
        return cartMapper.selectByExample(example);
    }


    public List<LeomallCart> queryByUidAndChecked(Integer userId) {
        LeomallCartExample example = new LeomallCartExample();
        example.or().andUserIdEqualTo(userId).andCheckedEqualTo(true).andDeletedEqualTo(false);
        return cartMapper.selectByExample(example);
    }

    public int delete(List<Integer> productIdList, int userId) {
        LeomallCartExample example = new LeomallCartExample();
        example.or().andUserIdEqualTo(userId).andProductIdIn(productIdList);
        return cartMapper.logicalDeleteByExample(example);
    }

    public LeomallCart findById(Integer id) {
        return cartMapper.selectByPrimaryKey(id);
    }

    public LeomallCart findById(Integer userId, Integer id) {
        LeomallCartExample example = new LeomallCartExample();
        example.or().andUserIdEqualTo(userId).andIdEqualTo(id).andDeletedEqualTo(false);
        return cartMapper.selectOneByExample(example);
    }

    public int updateCheck(Integer userId, List<Integer> idsList, Boolean checked) {
        LeomallCartExample example = new LeomallCartExample();
        example.or().andUserIdEqualTo(userId).andProductIdIn(idsList).andDeletedEqualTo(false);
        LeomallCart cart = new LeomallCart();
        cart.setChecked(checked);
        cart.setUpdateTime(LocalDateTime.now());
        return cartMapper.updateByExampleSelective(cart, example);
    }

    public void clearGoods(Integer userId) {
        LeomallCartExample example = new LeomallCartExample();
        example.or().andUserIdEqualTo(userId).andCheckedEqualTo(true);
        LeomallCart cart = new LeomallCart();
        cart.setDeleted(true);
        cartMapper.updateByExampleSelective(cart, example);
    }

    public List<LeomallCart> querySelective(Integer userId, Integer goodsId, Integer page, Integer limit, String sort, String order) {
        LeomallCartExample example = new LeomallCartExample();
        LeomallCartExample.Criteria criteria = example.createCriteria();

        if (!StringUtils.isEmpty(userId)) {
            criteria.andUserIdEqualTo(userId);
        }
        if (!StringUtils.isEmpty(goodsId)) {
            criteria.andGoodsIdEqualTo(goodsId);
        }
        criteria.andDeletedEqualTo(false);

        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            example.setOrderByClause(sort + " " + order);
        }

        PageHelper.startPage(page, limit);
        return cartMapper.selectByExample(example);
    }

    public void deleteById(Integer id) {
        cartMapper.logicalDeleteByPrimaryKey(id);
    }

    /** P1-2：带归属校验的逻辑删除，防止越权删除他人购物车 */
    public int deleteById(Integer userId, Integer id) {
        LeomallCart cart = new LeomallCart();
        cart.setDeleted(true);
        LeomallCartExample example = new LeomallCartExample();
        example.or().andIdEqualTo(id).andUserIdEqualTo(userId).andDeletedEqualTo(false);
        return cartMapper.updateByExampleSelective(cart, example);
    }

    public boolean checkExist(Integer goodsId) {
        LeomallCartExample example = new LeomallCartExample();
        example.or().andGoodsIdEqualTo(goodsId).andCheckedEqualTo(true).andDeletedEqualTo(false);
        return cartMapper.countByExample(example) != 0;
    }

    public void updateProduct(Integer id, String goodsSn, String goodsName, BigDecimal price, String url) {
        LeomallCart cart = new LeomallCart();
        cart.setPrice(price);
        cart.setPicUrl(url);
        cart.setGoodsSn(goodsSn);
        cart.setGoodsName(goodsName);
        LeomallCartExample example = new LeomallCartExample();
        example.or().andProductIdEqualTo(id);
        cartMapper.updateByExampleSelective(cart, example);
    }
}
