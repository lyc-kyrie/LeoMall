package com.leomall.db.service;

import com.github.pagehelper.PageHelper;
import com.leomall.db.dao.LeomallSearchHistoryMapper;
import com.leomall.db.domain.LeomallSearchHistory;
import com.leomall.db.domain.LeomallSearchHistoryExample;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeomallSearchHistoryService {
    @Resource
    private LeomallSearchHistoryMapper searchHistoryMapper;

    public void save(LeomallSearchHistory searchHistory) {
        searchHistory.setAddTime(LocalDateTime.now());
        searchHistory.setUpdateTime(LocalDateTime.now());
        searchHistoryMapper.insertSelective(searchHistory);
    }

    public List<LeomallSearchHistory> queryByUid(int uid) {
        LeomallSearchHistoryExample example = new LeomallSearchHistoryExample();
        example.or().andUserIdEqualTo(uid).andDeletedEqualTo(false);
        example.setDistinct(true);
        return searchHistoryMapper.selectByExampleSelective(example, LeomallSearchHistory.Column.keyword);
    }

    public void deleteByUid(int uid) {
        LeomallSearchHistoryExample example = new LeomallSearchHistoryExample();
        example.or().andUserIdEqualTo(uid);
        searchHistoryMapper.logicalDeleteByExample(example);
    }

    public List<LeomallSearchHistory> querySelective(String userId, String keyword, Integer page, Integer size, String sort, String order) {
        LeomallSearchHistoryExample example = new LeomallSearchHistoryExample();
        LeomallSearchHistoryExample.Criteria criteria = example.createCriteria();

        if (!StringUtils.isEmpty(userId)) {
            criteria.andUserIdEqualTo(Integer.valueOf(userId));
        }
        if (!StringUtils.isEmpty(keyword)) {
            criteria.andKeywordLike("%" + keyword + "%");
        }
        criteria.andDeletedEqualTo(false);

        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            example.setOrderByClause(sort + " " + order);
        }

        PageHelper.startPage(page, size);
        return searchHistoryMapper.selectByExample(example);
    }
}
