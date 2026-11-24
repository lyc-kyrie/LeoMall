package com.leomall.db.service;

import com.github.pagehelper.PageHelper;
import com.leomall.db.dao.LeomallTopicMapper;
import com.leomall.db.domain.LeomallGroupon;
import com.leomall.db.domain.LeomallTopic;
import com.leomall.db.domain.LeomallTopic.Column;
import com.leomall.db.domain.LeomallTopicExample;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeomallTopicService {
    @Resource
    private LeomallTopicMapper topicMapper;
    private Column[] columns = new Column[]{Column.id, Column.title, Column.subtitle, Column.price, Column.picUrl, Column.readCount};

    public List<LeomallTopic> queryList(int offset, int limit) {
        return queryList(offset, limit, "add_time", "desc");
    }

    public List<LeomallTopic> queryList(int offset, int limit, String sort, String order) {
        LeomallTopicExample example = new LeomallTopicExample();
        example.or().andDeletedEqualTo(false);
        example.setOrderByClause(sort + " " + order);
        PageHelper.startPage(offset, limit);
        return topicMapper.selectByExampleSelective(example, columns);
    }

    public int queryTotal() {
        LeomallTopicExample example = new LeomallTopicExample();
        example.or().andDeletedEqualTo(false);
        return (int) topicMapper.countByExample(example);
    }

    public LeomallTopic findById(Integer id) {
        LeomallTopicExample example = new LeomallTopicExample();
        example.or().andIdEqualTo(id).andDeletedEqualTo(false);
        return topicMapper.selectOneByExampleWithBLOBs(example);
    }

    public List<LeomallTopic> queryRelatedList(Integer id, int offset, int limit) {
        LeomallTopicExample example = new LeomallTopicExample();
        example.or().andIdEqualTo(id).andDeletedEqualTo(false);
        List<LeomallTopic> topics = topicMapper.selectByExample(example);
        if (topics.size() == 0) {
            return queryList(offset, limit, "add_time", "desc");
        }
        LeomallTopic topic = topics.get(0);

        example = new LeomallTopicExample();
        example.or().andIdNotEqualTo(topic.getId()).andDeletedEqualTo(false);
        PageHelper.startPage(offset, limit);
        List<LeomallTopic> relateds = topicMapper.selectByExampleWithBLOBs(example);
        if (relateds.size() != 0) {
            return relateds;
        }

        return queryList(offset, limit, "add_time", "desc");
    }

    public List<LeomallTopic> querySelective(String title, String subtitle, Integer page, Integer limit, String sort, String order) {
        LeomallTopicExample example = new LeomallTopicExample();
        LeomallTopicExample.Criteria criteria = example.createCriteria();

        if (!StringUtils.isEmpty(title)) {
            criteria.andTitleLike("%" + title + "%");
        }
        if (!StringUtils.isEmpty(subtitle)) {
            criteria.andSubtitleLike("%" + subtitle + "%");
        }
        criteria.andDeletedEqualTo(false);

        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            example.setOrderByClause(sort + " " + order);
        }

        PageHelper.startPage(page, limit);
        return topicMapper.selectByExampleWithBLOBs(example);
    }

    public int updateById(LeomallTopic topic) {
        topic.setUpdateTime(LocalDateTime.now());
        LeomallTopicExample example = new LeomallTopicExample();
        example.or().andIdEqualTo(topic.getId());
        return topicMapper.updateByExampleSelective(topic, example);
    }

    public void deleteById(Integer id) {
        topicMapper.logicalDeleteByPrimaryKey(id);
    }

    public void add(LeomallTopic topic) {
        topic.setAddTime(LocalDateTime.now());
        topic.setUpdateTime(LocalDateTime.now());
        topicMapper.insertSelective(topic);
    }


    public void deleteByIds(List<Integer> ids) {
        LeomallTopicExample example = new LeomallTopicExample();
        example.or().andIdIn(ids).andDeletedEqualTo(false);
        LeomallTopic topic = new LeomallTopic();
        topic.setUpdateTime(LocalDateTime.now());
        topic.setDeleted(true);
        topicMapper.updateByExampleSelective(topic, example);
    }
}
