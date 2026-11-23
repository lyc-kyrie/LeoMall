package com.leomall.db.service;

import com.github.pagehelper.PageHelper;
import com.leomall.db.dao.LeomallAgentRecommendationMapper;
import com.leomall.db.domain.LeomallAgentRecommendation;
import com.leomall.db.domain.LeomallAgentRecommendationExample;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/** 智能导购推荐记录服务（含点击埋点）。 */
@Service
public class LeomallAgentRecommendationService {

    @Resource
    private LeomallAgentRecommendationMapper recommendationMapper;

    public int add(LeomallAgentRecommendation record) {
        LocalDateTime now = LocalDateTime.now();
        record.setAddTime(now);
        record.setUpdateTime(now);
        record.setDeleted(false);
        if (record.getClicked() == null) {
            record.setClicked(false);
        }
        return recommendationMapper.insertSelective(record);
    }

    public void addBatch(List<LeomallAgentRecommendation> records) {
        for (LeomallAgentRecommendation record : records) {
            add(record);
        }
    }

    public List<LeomallAgentRecommendation> queryBySessionId(String sessionId) {
        LeomallAgentRecommendationExample example = new LeomallAgentRecommendationExample();
        example.or().andSessionIdEqualTo(sessionId).andDeletedEqualTo(false);
        example.setOrderByClause("id asc");
        return recommendationMapper.selectByExample(example);
    }

    public List<LeomallAgentRecommendation> queryByMessageId(Integer messageId) {
        LeomallAgentRecommendationExample example = new LeomallAgentRecommendationExample();
        example.or().andMessageIdEqualTo(messageId).andDeletedEqualTo(false);
        example.setOrderByClause("sort_rank asc, id asc");
        return recommendationMapper.selectByExample(example);
    }

    public List<LeomallAgentRecommendation> querySelective(String sessionId, Integer userId,
                                                             Integer page, Integer size) {
        LeomallAgentRecommendationExample example = new LeomallAgentRecommendationExample();
        LeomallAgentRecommendationExample.Criteria criteria = example.or();
        criteria.andDeletedEqualTo(false);
        if (!StringUtils.isEmpty(sessionId)) {
            criteria.andSessionIdEqualTo(sessionId);
        }
        if (userId != null) {
            criteria.andUserIdEqualTo(userId);
        }
        example.setOrderByClause("id asc");
        PageHelper.startPage(page, size);
        return recommendationMapper.selectByExample(example);
    }

    /** 点击埋点：只允许本人点击自己的推荐，防止越权改写。 */
    public int markClicked(Integer id, Integer userId) {
        if (id == null || userId == null) {
            return 0;
        }
        LeomallAgentRecommendation record = recommendationMapper.selectByPrimaryKey(id);
        if (record == null || !userId.equals(record.getUserId()) || Boolean.TRUE.equals(record.getDeleted())) {
            return 0;
        }
        LeomallAgentRecommendation patch = new LeomallAgentRecommendation();
        patch.setId(id);
        patch.setClicked(true);
        patch.setUpdateTime(LocalDateTime.now());
        return recommendationMapper.updateByPrimaryKeySelective(patch);
    }

    public long countAll() {
        LeomallAgentRecommendationExample example = new LeomallAgentRecommendationExample();
        example.or().andDeletedEqualTo(false);
        return recommendationMapper.countByExample(example);
    }

    public long countClicked() {
        LeomallAgentRecommendationExample example = new LeomallAgentRecommendationExample();
        example.or().andDeletedEqualTo(false).andClickedEqualTo(true);
        return recommendationMapper.countByExample(example);
    }
}
