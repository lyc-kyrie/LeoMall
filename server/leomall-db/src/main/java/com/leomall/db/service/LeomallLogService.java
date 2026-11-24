package com.leomall.db.service;

import com.github.pagehelper.PageHelper;
import com.leomall.db.dao.LeomallLogMapper;
import com.leomall.db.domain.LeomallAd;
import com.leomall.db.domain.LeomallLog;
import com.leomall.db.domain.LeomallLogExample;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeomallLogService {
    @Resource
    private LeomallLogMapper logMapper;

    public void deleteById(Integer id) {
        logMapper.logicalDeleteByPrimaryKey(id);
    }

    public void add(LeomallLog log) {
        log.setAddTime(LocalDateTime.now());
        log.setUpdateTime(LocalDateTime.now());
        logMapper.insertSelective(log);
    }

    public List<LeomallLog> querySelective(String name, Integer page, Integer size, String sort, String order) {
        LeomallLogExample example = new LeomallLogExample();
        LeomallLogExample.Criteria criteria = example.createCriteria();

        if (!StringUtils.isEmpty(name)) {
            criteria.andAdminLike("%" + name + "%");
        }
        criteria.andDeletedEqualTo(false);

        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            example.setOrderByClause(sort + " " + order);
        }

        PageHelper.startPage(page, size);
        return logMapper.selectByExample(example);
    }
}
