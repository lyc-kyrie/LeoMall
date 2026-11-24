package com.leomall.db.service;

import com.github.pagehelper.PageHelper;
import com.leomall.db.dao.LeomallBrandMapper;
import com.leomall.db.domain.LeomallBrand;
import com.leomall.db.domain.LeomallBrand.Column;
import com.leomall.db.domain.LeomallBrandExample;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeomallBrandService {
    @Resource
    private LeomallBrandMapper brandMapper;
    private Column[] columns = new Column[]{Column.id, Column.name, Column.desc, Column.picUrl, Column.floorPrice};

    public List<LeomallBrand> query(Integer page, Integer limit, String sort, String order) {
        LeomallBrandExample example = new LeomallBrandExample();
        example.or().andDeletedEqualTo(false);
        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            example.setOrderByClause(sort + " " + order);
        }
        PageHelper.startPage(page, limit);
        return brandMapper.selectByExampleSelective(example, columns);
    }

    public List<LeomallBrand> query(Integer page, Integer limit) {
        return query(page, limit, null, null);
    }

    public LeomallBrand findById(Integer id) {
        return brandMapper.selectByPrimaryKey(id);
    }

    public List<LeomallBrand> querySelective(String id, String name, Integer page, Integer size, String sort, String order) {
        LeomallBrandExample example = new LeomallBrandExample();
        LeomallBrandExample.Criteria criteria = example.createCriteria();

        if (!StringUtils.isEmpty(id)) {
            criteria.andIdEqualTo(Integer.valueOf(id));
        }
        if (!StringUtils.isEmpty(name)) {
            criteria.andNameLike("%" + name + "%");
        }
        criteria.andDeletedEqualTo(false);

        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            example.setOrderByClause(sort + " " + order);
        }

        PageHelper.startPage(page, size);
        return brandMapper.selectByExample(example);
    }

    public int updateById(LeomallBrand brand) {
        brand.setUpdateTime(LocalDateTime.now());
        return brandMapper.updateByPrimaryKeySelective(brand);
    }

    public void deleteById(Integer id) {
        brandMapper.logicalDeleteByPrimaryKey(id);
    }

    public void add(LeomallBrand brand) {
        brand.setAddTime(LocalDateTime.now());
        brand.setUpdateTime(LocalDateTime.now());
        brandMapper.insertSelective(brand);
    }

    public List<LeomallBrand> all() {
        LeomallBrandExample example = new LeomallBrandExample();
        example.or().andDeletedEqualTo(false);
        return brandMapper.selectByExample(example);
    }
}
