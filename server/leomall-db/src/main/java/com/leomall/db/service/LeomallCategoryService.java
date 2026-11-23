package com.leomall.db.service;

import com.github.pagehelper.PageHelper;
import com.leomall.db.dao.LeomallCategoryMapper;
import com.leomall.db.domain.LeomallCategory;
import com.leomall.db.domain.LeomallCategoryExample;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeomallCategoryService {
    @Resource
    private LeomallCategoryMapper categoryMapper;
    private LeomallCategory.Column[] CHANNEL = {LeomallCategory.Column.id, LeomallCategory.Column.name, LeomallCategory.Column.iconUrl};

    public List<LeomallCategory> queryL1WithoutRecommend(int offset, int limit) {
        LeomallCategoryExample example = new LeomallCategoryExample();
        example.or().andLevelEqualTo("L1").andNameNotEqualTo("推荐").andDeletedEqualTo(false);
        PageHelper.startPage(offset, limit);
        return categoryMapper.selectByExample(example);
    }

    public List<LeomallCategory> queryL1(int offset, int limit) {
        LeomallCategoryExample example = new LeomallCategoryExample();
        example.or().andLevelEqualTo("L1").andDeletedEqualTo(false);
        PageHelper.startPage(offset, limit);
        return categoryMapper.selectByExample(example);
    }

    public List<LeomallCategory> queryL1() {
        LeomallCategoryExample example = new LeomallCategoryExample();
        example.or().andLevelEqualTo("L1").andDeletedEqualTo(false);
        return categoryMapper.selectByExample(example);
    }

    public List<LeomallCategory> queryByPid(Integer pid) {
        LeomallCategoryExample example = new LeomallCategoryExample();
        example.or().andPidEqualTo(pid).andDeletedEqualTo(false);
        return categoryMapper.selectByExample(example);
    }

    public List<LeomallCategory> queryL2ByIds(List<Integer> ids) {
        LeomallCategoryExample example = new LeomallCategoryExample();
        example.or().andIdIn(ids).andLevelEqualTo("L2").andDeletedEqualTo(false);
        return categoryMapper.selectByExample(example);
    }

    public LeomallCategory findById(Integer id) {
        return categoryMapper.selectByPrimaryKey(id);
    }

    public List<LeomallCategory> querySelective(String id, String name, Integer page, Integer size, String sort, String order) {
        LeomallCategoryExample example = new LeomallCategoryExample();
        LeomallCategoryExample.Criteria criteria = example.createCriteria();

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
        return categoryMapper.selectByExample(example);
    }

    public int updateById(LeomallCategory category) {
        category.setUpdateTime(LocalDateTime.now());
        return categoryMapper.updateByPrimaryKeySelective(category);
    }

    public void deleteById(Integer id) {
        categoryMapper.logicalDeleteByPrimaryKey(id);
    }

    public void add(LeomallCategory category) {
        category.setAddTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.insertSelective(category);
    }

    public List<LeomallCategory> queryChannel() {
        LeomallCategoryExample example = new LeomallCategoryExample();
        example.or().andLevelEqualTo("L1").andDeletedEqualTo(false);
        return categoryMapper.selectByExampleSelective(example, CHANNEL);
    }
}
