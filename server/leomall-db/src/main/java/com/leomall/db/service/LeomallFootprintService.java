package com.leomall.db.service;

import com.github.pagehelper.PageHelper;
import com.leomall.db.dao.LeomallFootprintMapper;
import com.leomall.db.domain.LeomallFootprint;
import com.leomall.db.domain.LeomallFootprintExample;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeomallFootprintService {
    @Resource
    private LeomallFootprintMapper footprintMapper;

    public List<LeomallFootprint> queryByAddTime(Integer userId, Integer page, Integer size) {
        LeomallFootprintExample example = new LeomallFootprintExample();
        example.or().andUserIdEqualTo(userId).andDeletedEqualTo(false);
        example.setOrderByClause(LeomallFootprint.Column.addTime.desc());
        PageHelper.startPage(page, size);
        return footprintMapper.selectByExample(example);
    }

    public LeomallFootprint findById(Integer id) {
        return footprintMapper.selectByPrimaryKey(id);
    }

    public LeomallFootprint findById(Integer userId, Integer id) {
        LeomallFootprintExample example = new LeomallFootprintExample();
        example.or().andIdEqualTo(id).andUserIdEqualTo(userId).andDeletedEqualTo(false);
        return footprintMapper.selectOneByExample(example);
    }

    public void deleteById(Integer id) {
        footprintMapper.logicalDeleteByPrimaryKey(id);
    }

    public void add(LeomallFootprint footprint) {
        footprint.setAddTime(LocalDateTime.now());
        footprint.setUpdateTime(LocalDateTime.now());
        footprintMapper.insertSelective(footprint);
    }

    public List<LeomallFootprint> querySelective(String userId, String goodsId, Integer page, Integer size, String sort, String order) {
        LeomallFootprintExample example = new LeomallFootprintExample();
        LeomallFootprintExample.Criteria criteria = example.createCriteria();

        if (!StringUtils.isEmpty(userId)) {
            criteria.andUserIdEqualTo(Integer.valueOf(userId));
        }
        if (!StringUtils.isEmpty(goodsId)) {
            criteria.andGoodsIdEqualTo(Integer.valueOf(goodsId));
        }
        criteria.andDeletedEqualTo(false);

        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            example.setOrderByClause(sort + " " + order);
        }

        PageHelper.startPage(page, size);
        return footprintMapper.selectByExample(example);
    }
}
