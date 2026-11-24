package com.leomall.db.service;

import com.github.pagehelper.PageHelper;
import com.leomall.db.dao.LeomallIssueMapper;
import com.leomall.db.domain.LeomallIssue;
import com.leomall.db.domain.LeomallIssueExample;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeomallIssueService {
    @Resource
    private LeomallIssueMapper issueMapper;

    public void deleteById(Integer id) {
        issueMapper.logicalDeleteByPrimaryKey(id);
    }

    public void add(LeomallIssue issue) {
        issue.setAddTime(LocalDateTime.now());
        issue.setUpdateTime(LocalDateTime.now());
        issueMapper.insertSelective(issue);
    }

    public List<LeomallIssue> querySelective(String question, Integer page, Integer limit, String sort, String order) {
        LeomallIssueExample example = new LeomallIssueExample();
        LeomallIssueExample.Criteria criteria = example.createCriteria();

        if (!StringUtils.isEmpty(question)) {
            criteria.andQuestionLike("%" + question + "%");
        }
        criteria.andDeletedEqualTo(false);

        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            example.setOrderByClause(sort + " " + order);
        }

        PageHelper.startPage(page, limit);
        return issueMapper.selectByExample(example);
    }

    public int updateById(LeomallIssue issue) {
        issue.setUpdateTime(LocalDateTime.now());
        return issueMapper.updateByPrimaryKeySelective(issue);
    }

    public LeomallIssue findById(Integer id) {
        return issueMapper.selectByPrimaryKey(id);
    }
}
