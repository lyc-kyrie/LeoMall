package com.leomall.db.dao;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 智能导购统计查询（聚合类 SQL 不适合 MBG 生成，单独用注解式 Mapper）。
 * 由启动类上的 @MapperScan("com.leomall.db.dao") 自动注册。
 */
public interface AgentStatMapper {

    @Select("SELECT COUNT(DISTINCT user_id) FROM leomall_agent_session WHERE deleted = 0")
    long countAgentUsers();

    @Select("SELECT goods_id AS goodsId, MAX(goods_name) AS goodsName, COUNT(*) AS cnt "
            + "FROM leomall_agent_recommendation WHERE deleted = 0 "
            + "GROUP BY goods_id ORDER BY cnt DESC, goods_id ASC LIMIT #{limit}")
    List<Map<String, Object>> topRecommendGoods(@Param("limit") Integer limit);
}
