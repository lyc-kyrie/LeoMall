package com.leomall.db.service;

import com.github.pagehelper.PageHelper;
import com.leomall.db.dao.LeomallFeedbackMapper;
import com.leomall.db.domain.LeomallFeedback;
import com.leomall.db.domain.LeomallFeedbackExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @date 2018/8/27 11:39
 */
@Service
public class LeomallFeedbackService {
    @Autowired
    private LeomallFeedbackMapper feedbackMapper;

    public Integer add(LeomallFeedback feedback) {
        feedback.setAddTime(LocalDateTime.now());
        feedback.setUpdateTime(LocalDateTime.now());
        return feedbackMapper.insertSelective(feedback);
    }

    public List<LeomallFeedback> querySelective(Integer userId, String username, Integer page, Integer limit, String sort, String order) {
        LeomallFeedbackExample example = new LeomallFeedbackExample();
        LeomallFeedbackExample.Criteria criteria = example.createCriteria();

        if (userId != null) {
            criteria.andUserIdEqualTo(userId);
        }
        if (!StringUtils.isEmpty(username)) {
            criteria.andUsernameLike("%" + username + "%");
        }
        criteria.andDeletedEqualTo(false);

        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            example.setOrderByClause(sort + " " + order);
        }

        PageHelper.startPage(page, limit);
        return feedbackMapper.selectByExample(example);
    }
}
