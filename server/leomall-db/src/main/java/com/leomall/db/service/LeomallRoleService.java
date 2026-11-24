package com.leomall.db.service;

import com.alibaba.druid.util.StringUtils;
import com.github.pagehelper.PageHelper;
import com.leomall.db.dao.LeomallRoleMapper;
import com.leomall.db.domain.LeomallRole;
import com.leomall.db.domain.LeomallRoleExample;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class LeomallRoleService {
    @Resource
    private LeomallRoleMapper roleMapper;


    public Set<String> queryByIds(Integer[] roleIds) {
        Set<String> roles = new HashSet<String>();
        if(roleIds.length == 0){
            return roles;
        }

        LeomallRoleExample example = new LeomallRoleExample();
        example.or().andIdIn(Arrays.asList(roleIds)).andEnabledEqualTo(true).andDeletedEqualTo(false);
        List<LeomallRole> roleList = roleMapper.selectByExample(example);

        for(LeomallRole role : roleList){
            roles.add(role.getName());
        }

        return roles;

    }

    public List<LeomallRole> querySelective(String name, Integer page, Integer limit, String sort, String order) {
        LeomallRoleExample example = new LeomallRoleExample();
        LeomallRoleExample.Criteria criteria = example.createCriteria();

        if (!StringUtils.isEmpty(name)) {
            criteria.andNameLike("%" + name + "%");
        }
        criteria.andDeletedEqualTo(false);

        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            example.setOrderByClause(sort + " " + order);
        }

        PageHelper.startPage(page, limit);
        return roleMapper.selectByExample(example);
    }

    public LeomallRole findById(Integer id) {
        return roleMapper.selectByPrimaryKey(id);
    }

    public void add(LeomallRole role) {
        role.setAddTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.insertSelective(role);
    }

    public void deleteById(Integer id) {
        roleMapper.logicalDeleteByPrimaryKey(id);
    }

    public void updateById(LeomallRole role) {
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.updateByPrimaryKeySelective(role);
    }

    public boolean checkExist(String name) {
        LeomallRoleExample example = new LeomallRoleExample();
        example.or().andNameEqualTo(name).andDeletedEqualTo(false);
        return roleMapper.countByExample(example) != 0;
    }

    public List<LeomallRole> queryAll() {
        LeomallRoleExample example = new LeomallRoleExample();
        example.or().andDeletedEqualTo(false);
        return roleMapper.selectByExample(example);
    }
}
