package com.leomall.db.service;

import com.github.pagehelper.PageHelper;
import com.leomall.db.dao.LeomallAdminMapper;
import com.leomall.db.domain.LeomallAdmin;
import com.leomall.db.domain.LeomallAdmin.Column;
import com.leomall.db.domain.LeomallAdminExample;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeomallAdminService {
    private final Column[] result = new Column[]{Column.id, Column.username, Column.avatar, Column.roleIds};
    @Resource
    private LeomallAdminMapper adminMapper;

    public List<LeomallAdmin> findAdmin(String username) {
        LeomallAdminExample example = new LeomallAdminExample();
        example.or().andUsernameEqualTo(username).andDeletedEqualTo(false);
        return adminMapper.selectByExample(example);
    }

    public LeomallAdmin findAdmin(Integer id) {
        return adminMapper.selectByPrimaryKey(id);
    }

    public List<LeomallAdmin> querySelective(String username, Integer page, Integer limit, String sort, String order) {
        LeomallAdminExample example = new LeomallAdminExample();
        LeomallAdminExample.Criteria criteria = example.createCriteria();

        if (!StringUtils.isEmpty(username)) {
            criteria.andUsernameLike("%" + username + "%");
        }
        criteria.andDeletedEqualTo(false);

        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            example.setOrderByClause(sort + " " + order);
        }

        PageHelper.startPage(page, limit);
        return adminMapper.selectByExampleSelective(example, result);
    }

    public int updateById(LeomallAdmin admin) {
        admin.setUpdateTime(LocalDateTime.now());
        return adminMapper.updateByPrimaryKeySelective(admin);
    }

    public void deleteById(Integer id) {
        adminMapper.logicalDeleteByPrimaryKey(id);
    }

    public void add(LeomallAdmin admin) {
        admin.setAddTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());
        adminMapper.insertSelective(admin);
    }

    public LeomallAdmin findById(Integer id) {
        return adminMapper.selectByPrimaryKeySelective(id, result);
    }

    public List<LeomallAdmin> all() {
        LeomallAdminExample example = new LeomallAdminExample();
        example.or().andDeletedEqualTo(false);
        return adminMapper.selectByExample(example);
    }
}
