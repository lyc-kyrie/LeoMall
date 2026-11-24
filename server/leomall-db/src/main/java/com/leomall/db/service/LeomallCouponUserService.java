package com.leomall.db.service;

import com.github.pagehelper.PageHelper;
import com.leomall.db.dao.LeomallCouponUserMapper;
import com.leomall.db.domain.LeomallCouponUser;
import com.leomall.db.domain.LeomallCouponUserExample;
import com.leomall.db.util.CouponUserConstant;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeomallCouponUserService {
    @Resource
    private LeomallCouponUserMapper couponUserMapper;

    public Integer countCoupon(Integer couponId) {
        LeomallCouponUserExample example = new LeomallCouponUserExample();
        example.or().andCouponIdEqualTo(couponId).andDeletedEqualTo(false);
        return (int)couponUserMapper.countByExample(example);
    }

    public Integer countUserAndCoupon(Integer userId, Integer couponId) {
        LeomallCouponUserExample example = new LeomallCouponUserExample();
        example.or().andUserIdEqualTo(userId).andCouponIdEqualTo(couponId).andDeletedEqualTo(false);
        return (int)couponUserMapper.countByExample(example);
    }

    public void add(LeomallCouponUser couponUser) {
        couponUser.setAddTime(LocalDateTime.now());
        couponUser.setUpdateTime(LocalDateTime.now());
        couponUserMapper.insertSelective(couponUser);
    }

    public List<LeomallCouponUser> queryList(Integer userId, Integer couponId, Short status, Integer page, Integer size, String sort, String order) {
        LeomallCouponUserExample example = new LeomallCouponUserExample();
        LeomallCouponUserExample.Criteria criteria = example.createCriteria();
        if (userId != null) {
            criteria.andUserIdEqualTo(userId);
        }
        if(couponId != null){
            criteria.andCouponIdEqualTo(couponId);
        }
        if (status != null) {
            criteria.andStatusEqualTo(status);
        }
        criteria.andDeletedEqualTo(false);

        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            example.setOrderByClause(sort + " " + order);
        }

        if (!StringUtils.isEmpty(page) && !StringUtils.isEmpty(size)) {
            PageHelper.startPage(page, size);
        }

        return couponUserMapper.selectByExample(example);
    }

    public List<LeomallCouponUser> queryAll(Integer userId, Integer couponId) {
        return queryList(userId, couponId, CouponUserConstant.STATUS_USABLE, null, null, "add_time", "desc");
    }

    public List<LeomallCouponUser> queryAll(Integer userId) {
        return queryList(userId, null, CouponUserConstant.STATUS_USABLE, null, null, "add_time", "desc");
    }

    public LeomallCouponUser queryOne(Integer userId, Integer couponId) {
        List<LeomallCouponUser> couponUserList = queryList(userId, couponId, CouponUserConstant.STATUS_USABLE, 1, 1, "add_time", "desc");
        if(couponUserList.size() == 0){
            return null;
        }
        return couponUserList.get(0);
    }

    public LeomallCouponUser findById(Integer id) {
        return couponUserMapper.selectByPrimaryKey(id);
    }


    public int update(LeomallCouponUser couponUser) {
        couponUser.setUpdateTime(LocalDateTime.now());
        return couponUserMapper.updateByPrimaryKeySelective(couponUser);
    }

    /**
     * P0-2：原子核销优惠券。
     * 仅当「券属于该用户 + 未删除 + 仍可用」时才置为已用，返回影响行数；
     * 调用方（下单事务）在返回 0 时回滚整单，杜绝并发复用与越权核销。
     */
    public int markUsed(Integer userId, Integer id, Integer orderId) {
        LeomallCouponUser record = new LeomallCouponUser();
        record.setStatus(CouponUserConstant.STATUS_USED);
        record.setUsedTime(LocalDateTime.now());
        record.setOrderId(orderId);
        record.setUpdateTime(LocalDateTime.now());

        LeomallCouponUserExample example = new LeomallCouponUserExample();
        example.or().andIdEqualTo(id).andUserIdEqualTo(userId)
                .andStatusEqualTo(CouponUserConstant.STATUS_USABLE).andDeletedEqualTo(false);
        return couponUserMapper.updateByExampleSelective(record, example);
    }

    public List<LeomallCouponUser> queryExpired() {
        LeomallCouponUserExample example = new LeomallCouponUserExample();
        example.or().andStatusEqualTo(CouponUserConstant.STATUS_USABLE).andEndTimeLessThan(LocalDateTime.now()).andDeletedEqualTo(false);
        return couponUserMapper.selectByExample(example);
    }

    public List<LeomallCouponUser> findByOid(Integer orderId) {
        LeomallCouponUserExample example = new LeomallCouponUserExample();
        example.or().andOrderIdEqualTo(orderId).andDeletedEqualTo(false);
        return couponUserMapper.selectByExample(example);
    }
}
