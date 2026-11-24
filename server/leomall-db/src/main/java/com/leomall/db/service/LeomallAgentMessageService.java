package com.leomall.db.service;

import com.github.pagehelper.PageHelper;
import com.leomall.db.dao.LeomallAgentMessageMapper;
import com.leomall.db.domain.LeomallAgentMessage;
import com.leomall.db.domain.LeomallAgentMessageExample;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/** 智能导购逐轮消息服务。 */
@Service
public class LeomallAgentMessageService {

    @Resource
    private LeomallAgentMessageMapper messageMapper;

    public int add(LeomallAgentMessage message) {
        LocalDateTime now = LocalDateTime.now();
        message.setAddTime(now);
        message.setUpdateTime(now);
        message.setDeleted(false);
        return messageMapper.insertSelective(message);
    }

    public LeomallAgentMessage findById(Integer id) {
        return id == null ? null : messageMapper.selectByPrimaryKey(id);
    }

    public List<LeomallAgentMessage> queryBySessionId(String sessionId) {
        LeomallAgentMessageExample example = new LeomallAgentMessageExample();
        example.or().andSessionIdEqualTo(sessionId).andDeletedEqualTo(false);
        example.setOrderByClause("id asc");
        return messageMapper.selectByExample(example);
    }

    /**
     * P3-6：只取会话最近 limit 条消息（SQL LIMIT），避免长会话全量加载后再截断。
     * 返回仍按 id 升序（时间正序），与 queryBySessionId 的语义一致。
     */
    public List<LeomallAgentMessage> queryRecentBySessionId(String sessionId, int limit) {
        LeomallAgentMessageExample example = new LeomallAgentMessageExample();
        example.or().andSessionIdEqualTo(sessionId).andDeletedEqualTo(false);
        example.setOrderByClause("id desc");
        PageHelper.startPage(1, Math.max(1, limit), false);
        List<LeomallAgentMessage> rows = messageMapper.selectByExample(example);
        Collections.reverse(rows);
        return rows;
    }

    public List<LeomallAgentMessage> querySelective(String sessionId, String userId,
                                                     Integer page, Integer size,
                                                     String sort, String order) {
        LeomallAgentMessageExample example = new LeomallAgentMessageExample();
        LeomallAgentMessageExample.Criteria criteria = example.or();
        criteria.andDeletedEqualTo(false);
        if (!StringUtils.isEmpty(sessionId)) {
            criteria.andSessionIdEqualTo(sessionId);
        }
        if (!StringUtils.isEmpty(userId)) {
            criteria.andUserIdEqualTo(Integer.valueOf(userId));
        }
        example.setOrderByClause(
                (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order))
                        ? sort + " " + order : "id asc");
        PageHelper.startPage(page, size);
        return messageMapper.selectByExample(example);
    }

    public long countAll() {
        LeomallAgentMessageExample example = new LeomallAgentMessageExample();
        example.or().andDeletedEqualTo(false);
        return messageMapper.countByExample(example);
    }
}
