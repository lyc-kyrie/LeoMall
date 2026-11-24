package com.leomall.db.service;

import com.github.pagehelper.PageHelper;
import com.leomall.db.dao.LeomallNoticeAdminMapper;
import com.leomall.db.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeomallNoticeAdminService {
    @Resource
    private LeomallNoticeAdminMapper noticeAdminMapper;

    public List<LeomallNoticeAdmin> querySelective(String title, String type, Integer adminId, Integer page, Integer limit, String sort, String order) {
        LeomallNoticeAdminExample example = new LeomallNoticeAdminExample();
        LeomallNoticeAdminExample.Criteria criteria = example.createCriteria();

        if(!StringUtils.isEmpty(title)){
            criteria.andNoticeTitleLike("%" + title + "%");
        }

        if(type.equals("read")){
         criteria.andReadTimeIsNotNull();
        }
        else if(type.equals("unread")){
            criteria.andReadTimeIsNull();
        }
        criteria.andAdminIdEqualTo(adminId);
        criteria.andDeletedEqualTo(false);

        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            example.setOrderByClause(sort + " " + order);
        }

        PageHelper.startPage(page, limit);
        return noticeAdminMapper.selectByExample(example);
    }

    public LeomallNoticeAdmin find(Integer noticeId, Integer adminId) {
        LeomallNoticeAdminExample example = new LeomallNoticeAdminExample();
        example.or().andNoticeIdEqualTo(noticeId).andAdminIdEqualTo(adminId).andDeletedEqualTo(false);
        return noticeAdminMapper.selectOneByExample(example);
    }

    public void add(LeomallNoticeAdmin noticeAdmin) {
        noticeAdmin.setAddTime(LocalDateTime.now());
        noticeAdmin.setUpdateTime(LocalDateTime.now());
        noticeAdminMapper.insertSelective(noticeAdmin);
    }

    public void update(LeomallNoticeAdmin noticeAdmin) {
        noticeAdmin.setUpdateTime(LocalDateTime.now());
        noticeAdminMapper.updateByPrimaryKeySelective(noticeAdmin);
    }

    public void markReadByIds(List<Integer> ids, Integer adminId) {
        LeomallNoticeAdminExample example = new LeomallNoticeAdminExample();
        example.or().andIdIn(ids).andAdminIdEqualTo(adminId).andDeletedEqualTo(false);
        LeomallNoticeAdmin noticeAdmin = new LeomallNoticeAdmin();
        LocalDateTime now = LocalDateTime.now();
        noticeAdmin.setReadTime(now);
        noticeAdmin.setUpdateTime(now);
        noticeAdminMapper.updateByExampleSelective(noticeAdmin, example);
    }

    public void deleteById(Integer id, Integer adminId) {
        LeomallNoticeAdminExample example = new LeomallNoticeAdminExample();
        example.or().andIdEqualTo(id).andAdminIdEqualTo(adminId).andDeletedEqualTo(false);
        LeomallNoticeAdmin noticeAdmin = new LeomallNoticeAdmin();
        noticeAdmin.setUpdateTime(LocalDateTime.now());
        noticeAdmin.setDeleted(true);
        noticeAdminMapper.updateByExampleSelective(noticeAdmin, example);
    }

    public void deleteByIds(List<Integer> ids, Integer adminId) {
        LeomallNoticeAdminExample example = new LeomallNoticeAdminExample();
        example.or().andIdIn(ids).andAdminIdEqualTo(adminId).andDeletedEqualTo(false);
        LeomallNoticeAdmin noticeAdmin = new LeomallNoticeAdmin();
        noticeAdmin.setUpdateTime(LocalDateTime.now());
        noticeAdmin.setDeleted(true);
        noticeAdminMapper.updateByExampleSelective(noticeAdmin, example);
    }

    public int countUnread(Integer adminId) {
        LeomallNoticeAdminExample example = new LeomallNoticeAdminExample();
        example.or().andAdminIdEqualTo(adminId).andReadTimeIsNull().andDeletedEqualTo(false);
        return (int)noticeAdminMapper.countByExample(example);
    }

    public List<LeomallNoticeAdmin> queryByNoticeId(Integer noticeId) {
        LeomallNoticeAdminExample example = new LeomallNoticeAdminExample();
        example.or().andNoticeIdEqualTo(noticeId).andDeletedEqualTo(false);
        return noticeAdminMapper.selectByExample(example);
    }

    public void deleteByNoticeId(Integer id) {
        LeomallNoticeAdminExample example = new LeomallNoticeAdminExample();
        example.or().andNoticeIdEqualTo(id).andDeletedEqualTo(false);
        LeomallNoticeAdmin noticeAdmin = new LeomallNoticeAdmin();
        noticeAdmin.setUpdateTime(LocalDateTime.now());
        noticeAdmin.setDeleted(true);
        noticeAdminMapper.updateByExampleSelective(noticeAdmin, example);
    }

    public void deleteByNoticeIds(List<Integer> ids) {
        LeomallNoticeAdminExample example = new LeomallNoticeAdminExample();
        example.or().andNoticeIdIn(ids).andDeletedEqualTo(false);
        LeomallNoticeAdmin noticeAdmin = new LeomallNoticeAdmin();
        noticeAdmin.setUpdateTime(LocalDateTime.now());
        noticeAdmin.setDeleted(true);
        noticeAdminMapper.updateByExampleSelective(noticeAdmin, example);
    }

    public int countReadByNoticeId(Integer noticeId) {
        LeomallNoticeAdminExample example = new LeomallNoticeAdminExample();
        example.or().andNoticeIdEqualTo(noticeId).andReadTimeIsNotNull().andDeletedEqualTo(false);
        return (int)noticeAdminMapper.countByExample(example);
    }

    public void updateByNoticeId(LeomallNoticeAdmin noticeAdmin, Integer noticeId) {
        LeomallNoticeAdminExample example = new LeomallNoticeAdminExample();
        example.or().andNoticeIdEqualTo(noticeId).andDeletedEqualTo(false);
        noticeAdmin.setUpdateTime(LocalDateTime.now());
        noticeAdminMapper.updateByExampleSelective(noticeAdmin, example);
    }
}
