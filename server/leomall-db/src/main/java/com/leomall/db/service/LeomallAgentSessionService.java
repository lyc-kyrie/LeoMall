package com.leomall.db.service;

import com.github.pagehelper.PageHelper;
import com.leomall.db.dao.LeomallAgentSessionMapper;
import com.leomall.db.domain.LeomallAgentSession;
import com.leomall.db.domain.LeomallAgentSessionExample;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能导购会话服务。
 * 会话以 session_id 为业务唯一键；用户维度隔离在 Controller 层通过 userId 校验。
 */
@Service
public class LeomallAgentSessionService {

    @Resource
    private LeomallAgentSessionMapper sessionMapper;

    public LeomallAgentSession findBySessionId(String sessionId) {
        if (StringUtils.isEmpty(sessionId)) {
            return null;
        }
        LeomallAgentSessionExample example = new LeomallAgentSessionExample();
        example.or().andSessionIdEqualTo(sessionId).andDeletedEqualTo(false);
        return sessionMapper.selectOneByExample(example);
    }

    public LeomallAgentSession findBySessionIdAndUser(String sessionId, Integer userId) {
        if (StringUtils.isEmpty(sessionId) || userId == null) {
            return null;
        }
        LeomallAgentSessionExample example = new LeomallAgentSessionExample();
        example.or().andSessionIdEqualTo(sessionId).andUserIdEqualTo(userId).andDeletedEqualTo(false);
        return sessionMapper.selectOneByExample(example);
    }

    public List<LeomallAgentSession> queryByUid(Integer userId) {
        LeomallAgentSessionExample example = new LeomallAgentSessionExample();
        example.or().andUserIdEqualTo(userId).andDeletedEqualTo(false);
        example.setOrderByClause("add_time desc");
        return sessionMapper.selectByExample(example);
    }

    public int add(LeomallAgentSession session) {
        LocalDateTime now = LocalDateTime.now();
        session.setAddTime(now);
        session.setUpdateTime(now);
        session.setDeleted(false);
        return sessionMapper.insertSelective(session);
    }

    public int updateById(LeomallAgentSession session) {
        session.setUpdateTime(LocalDateTime.now());
        return sessionMapper.updateByPrimaryKeySelective(session);
    }

    public void deleteById(Integer id) {
        sessionMapper.logicalDeleteByPrimaryKey(id);
    }

    public List<LeomallAgentSession> querySelective(String userId, String keyword,
                                                     Integer page, Integer size,
                                                     String sort, String order) {
        LeomallAgentSessionExample example = new LeomallAgentSessionExample();
        if (StringUtils.isEmpty(keyword)) {
            applyCommon(example.or(), userId);
        } else {
            // 标题或最近提问命中，两个分支都要带上 userId / deleted 约束
            applyCommon(example.or(), userId).andTitleLike("%" + keyword + "%");
            applyCommon(example.or(), userId).andLastQueryLike("%" + keyword + "%");
        }
        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            example.setOrderByClause(sort + " " + order);
        }
        PageHelper.startPage(page, size);
        return sessionMapper.selectByExample(example);
    }

    private LeomallAgentSessionExample.Criteria applyCommon(
            LeomallAgentSessionExample.Criteria criteria, String userId) {
        criteria.andDeletedEqualTo(false);
        if (!StringUtils.isEmpty(userId)) {
            criteria.andUserIdEqualTo(Integer.valueOf(userId));
        }
        return criteria;
    }

    public long countAll() {
        LeomallAgentSessionExample example = new LeomallAgentSessionExample();
        example.or().andDeletedEqualTo(false);
        return sessionMapper.countByExample(example);
    }
}
