package com.leomall.db.service;

import com.alibaba.druid.util.StringUtils;
import com.github.pagehelper.PageHelper;
import com.leomall.db.dao.LeomallGoodsMapper;
import com.leomall.db.dao.LeomallGrouponRulesMapper;
import com.leomall.db.domain.LeomallGoods;
import com.leomall.db.domain.LeomallGrouponRules;
import com.leomall.db.domain.LeomallGrouponRulesExample;
import com.leomall.db.util.GrouponConstant;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LeomallGrouponRulesService {
    @Resource
    private LeomallGrouponRulesMapper mapper;
    @Resource
    private LeomallGoodsMapper goodsMapper;
    private LeomallGoods.Column[] goodsColumns = new LeomallGoods.Column[]{LeomallGoods.Column.id, LeomallGoods.Column.name, LeomallGoods.Column.brief, LeomallGoods.Column.picUrl, LeomallGoods.Column.counterPrice, LeomallGoods.Column.retailPrice};

    public int createRules(LeomallGrouponRules rules) {
        rules.setAddTime(LocalDateTime.now());
        rules.setUpdateTime(LocalDateTime.now());
        return mapper.insertSelective(rules);
    }

    /**
     * 根据ID查找对应团购项
     *
     * @param id
     * @return
     */
    public LeomallGrouponRules findById(Integer id) {
        return mapper.selectByPrimaryKey(id);
    }

    /**
     * 查询某个商品关联的团购规则
     *
     * @param goodsId
     * @return
     */
    public List<LeomallGrouponRules> queryByGoodsId(Integer goodsId) {
        LeomallGrouponRulesExample example = new LeomallGrouponRulesExample();
        example.or().andGoodsIdEqualTo(goodsId).andStatusEqualTo(GrouponConstant.RULE_STATUS_ON).andDeletedEqualTo(false);
        return mapper.selectByExample(example);
    }

    public int countByGoodsId(Integer goodsId) {
        LeomallGrouponRulesExample example = new LeomallGrouponRulesExample();
        example.or().andGoodsIdEqualTo(goodsId).andStatusEqualTo(GrouponConstant.RULE_STATUS_ON).andDeletedEqualTo(false);
        return (int)mapper.countByExample(example);
    }

    public List<LeomallGrouponRules> queryByStatus(Short status) {
        LeomallGrouponRulesExample example = new LeomallGrouponRulesExample();
        example.or().andStatusEqualTo(status).andDeletedEqualTo(false);
        return mapper.selectByExample(example);
    }

    /**
     * 获取首页团购规则列表
     *
     * @param page
     * @param limit
     * @return
     */
    public List<LeomallGrouponRules> queryList(Integer page, Integer limit) {
        return queryList(page, limit, "add_time", "desc");
    }

    public List<LeomallGrouponRules> queryList(Integer page, Integer limit, String sort, String order) {
        LeomallGrouponRulesExample example = new LeomallGrouponRulesExample();
        example.or().andStatusEqualTo(GrouponConstant.RULE_STATUS_ON).andDeletedEqualTo(false);
        example.setOrderByClause(sort + " " + order);
        PageHelper.startPage(page, limit);
        return mapper.selectByExample(example);
    }

    /**
     * 判断某个团购规则是否已经过期
     *
     * @return
     */
    public boolean isExpired(LeomallGrouponRules rules) {
        return (rules == null || rules.getExpireTime().isBefore(LocalDateTime.now()));
    }

    /**
     * 获取团购规则列表
     *
     * @param goodsId
     * @param page
     * @param size
     * @param sort
     * @param order
     * @return
     */
    public List<LeomallGrouponRules> querySelective(String goodsId, Integer page, Integer size, String sort, String order) {
        LeomallGrouponRulesExample example = new LeomallGrouponRulesExample();
        example.setOrderByClause(sort + " " + order);

        LeomallGrouponRulesExample.Criteria criteria = example.createCriteria();

        if (!StringUtils.isEmpty(goodsId)) {
            criteria.andGoodsIdEqualTo(Integer.parseInt(goodsId));
        }
        criteria.andDeletedEqualTo(false);

        PageHelper.startPage(page, size);
        return mapper.selectByExample(example);
    }

    public void delete(Integer id) {
        mapper.logicalDeleteByPrimaryKey(id);
    }

    public int updateById(LeomallGrouponRules grouponRules) {
        grouponRules.setUpdateTime(LocalDateTime.now());
        return mapper.updateByPrimaryKeySelective(grouponRules);
    }
}