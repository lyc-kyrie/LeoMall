package com.leomall.db.service;

import com.github.pagehelper.PageHelper;
import com.leomall.db.dao.LeomallRegionMapper;
import com.leomall.db.domain.LeomallRegion;
import com.leomall.db.domain.LeomallRegionExample;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class LeomallRegionService {

    @Resource
    private LeomallRegionMapper regionMapper;

    public List<LeomallRegion> getAll(){
        LeomallRegionExample example = new LeomallRegionExample();
        byte b = 4;
        example.or().andTypeNotEqualTo(b);
        return regionMapper.selectByExample(example);
    }

    public List<LeomallRegion> queryByPid(Integer parentId) {
        LeomallRegionExample example = new LeomallRegionExample();
        example.or().andPidEqualTo(parentId);
        return regionMapper.selectByExample(example);
    }

    public LeomallRegion findById(Integer id) {
        return regionMapper.selectByPrimaryKey(id);
    }

    public List<LeomallRegion> querySelective(String name, Integer code, Integer page, Integer size, String sort, String order) {
        LeomallRegionExample example = new LeomallRegionExample();
        LeomallRegionExample.Criteria criteria = example.createCriteria();

        if (!StringUtils.isEmpty(name)) {
            criteria.andNameLike("%" + name + "%");
        }
        if (!StringUtils.isEmpty(code)) {
            criteria.andCodeEqualTo(code);
        }

        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            example.setOrderByClause(sort + " " + order);
        }

        PageHelper.startPage(page, size);
        return regionMapper.selectByExample(example);
    }

}
