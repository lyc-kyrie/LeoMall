package com.leomall.db.service;

import com.github.pagehelper.PageHelper;
import com.leomall.db.dao.LeomallCollectMapper;
import com.leomall.db.domain.LeomallCollect;
import com.leomall.db.domain.LeomallCollectExample;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeomallCollectService {
    @Resource
    private LeomallCollectMapper collectMapper;

    public int count(int uid, byte type, Integer gid) {
        LeomallCollectExample example = new LeomallCollectExample();
        example.or().andUserIdEqualTo(uid).andTypeEqualTo(type).andValueIdEqualTo(gid).andDeletedEqualTo(false);
        return (int) collectMapper.countByExample(example);
    }

    public List<LeomallCollect> queryByType(Integer userId, Byte type, Integer page, Integer limit, String sort, String order) {
        LeomallCollectExample example = new LeomallCollectExample();
        LeomallCollectExample.Criteria criteria = example.createCriteria();

        if (type != null) {
            criteria.andTypeEqualTo(type);
        }
        criteria.andUserIdEqualTo(userId);
        criteria.andDeletedEqualTo(false);

        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            example.setOrderByClause(sort + " " + order);
        }

        PageHelper.startPage(page, limit);
        return collectMapper.selectByExample(example);
    }

    public int countByType(Integer userId, Byte type) {
        LeomallCollectExample example = new LeomallCollectExample();
        example.or().andUserIdEqualTo(userId).andTypeEqualTo(type).andDeletedEqualTo(false);
        return (int) collectMapper.countByExample(example);
    }

    public LeomallCollect queryByTypeAndValue(Integer userId, Byte type, Integer valueId) {
        LeomallCollectExample example = new LeomallCollectExample();
        example.or().andUserIdEqualTo(userId).andValueIdEqualTo(valueId).andTypeEqualTo(type).andDeletedEqualTo(false);
        return collectMapper.selectOneByExample(example);
    }

    public void deleteById(Integer id) {
        collectMapper.logicalDeleteByPrimaryKey(id);
    }

    public int add(LeomallCollect collect) {
        collect.setAddTime(LocalDateTime.now());
        collect.setUpdateTime(LocalDateTime.now());
        return collectMapper.insertSelective(collect);
    }

    public List<LeomallCollect> querySelective(String userId, String valueId, Integer page, Integer size, String sort, String order) {
        LeomallCollectExample example = new LeomallCollectExample();
        LeomallCollectExample.Criteria criteria = example.createCriteria();

        if (!StringUtils.isEmpty(userId)) {
            criteria.andUserIdEqualTo(Integer.valueOf(userId));
        }
        if (!StringUtils.isEmpty(valueId)) {
            criteria.andValueIdEqualTo(Integer.valueOf(valueId));
        }
        criteria.andDeletedEqualTo(false);

        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            example.setOrderByClause(sort + " " + order);
        }

        PageHelper.startPage(page, size);
        return collectMapper.selectByExample(example);
    }
}
