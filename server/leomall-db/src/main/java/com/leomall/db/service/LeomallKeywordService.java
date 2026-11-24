package com.leomall.db.service;

import com.github.pagehelper.PageHelper;
import com.leomall.db.dao.LeomallKeywordMapper;
import com.leomall.db.domain.LeomallKeyword;
import com.leomall.db.domain.LeomallKeywordExample;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeomallKeywordService {
    @Resource
    private LeomallKeywordMapper keywordsMapper;

    public LeomallKeyword queryDefault() {
        LeomallKeywordExample example = new LeomallKeywordExample();
        example.or().andIsDefaultEqualTo(true).andDeletedEqualTo(false);
        return keywordsMapper.selectOneByExample(example);
    }

    public List<LeomallKeyword> queryHots() {
        LeomallKeywordExample example = new LeomallKeywordExample();
        example.or().andIsHotEqualTo(true).andDeletedEqualTo(false);
        return keywordsMapper.selectByExample(example);
    }

    public List<LeomallKeyword> queryByKeyword(String keyword, Integer page, Integer limit) {
        LeomallKeywordExample example = new LeomallKeywordExample();
        example.setDistinct(true);
        example.or().andKeywordLike("%" + keyword + "%").andDeletedEqualTo(false);
        PageHelper.startPage(page, limit);
        return keywordsMapper.selectByExampleSelective(example, LeomallKeyword.Column.keyword);
    }

    public List<LeomallKeyword> querySelective(String keyword, String url, Integer page, Integer limit, String sort, String order) {
        LeomallKeywordExample example = new LeomallKeywordExample();
        LeomallKeywordExample.Criteria criteria = example.createCriteria();

        if (!StringUtils.isEmpty(keyword)) {
            criteria.andKeywordLike("%" + keyword + "%");
        }
        if (!StringUtils.isEmpty(url)) {
            criteria.andUrlLike("%" + url + "%");
        }
        criteria.andDeletedEqualTo(false);

        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            example.setOrderByClause(sort + " " + order);
        }

        PageHelper.startPage(page, limit);
        return keywordsMapper.selectByExample(example);
    }

    public void add(LeomallKeyword keywords) {
        keywords.setAddTime(LocalDateTime.now());
        keywords.setUpdateTime(LocalDateTime.now());
        keywordsMapper.insertSelective(keywords);
    }

    public LeomallKeyword findById(Integer id) {
        return keywordsMapper.selectByPrimaryKey(id);
    }

    public int updateById(LeomallKeyword keywords) {
        keywords.setUpdateTime(LocalDateTime.now());
        return keywordsMapper.updateByPrimaryKeySelective(keywords);
    }

    public void deleteById(Integer id) {
        keywordsMapper.logicalDeleteByPrimaryKey(id);
    }
}
