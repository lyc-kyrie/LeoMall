package com.leomall.admin.job;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.leomall.core.system.SystemConfig;
import com.leomall.db.domain.*;
import com.leomall.db.service.*;
import com.leomall.db.util.CouponUserConstant;
import com.leomall.db.util.OrderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 检测订单状态
 */
@Component
public class OrderJob {
    private final Log logger = LogFactory.getLog(OrderJob.class);

    @Autowired
    private LeomallOrderGoodsService orderGoodsService;
    @Autowired
    private LeomallOrderService orderService;
    @Autowired
    private LeomallGoodsProductService productService;
    @Autowired
    private LeomallGrouponService grouponService;
    @Autowired
    private LeomallGrouponRulesService rulesService;
    @Autowired
    private LeomallCouponUserService couponUserService;

    /**
     * P2-2：未支付订单超时取消的 DB 扫描兜底。
     *
     * 原实现仅靠内存延迟队列（OrderUnpaidTask）+ 启动重挂，任务异常丢失或
     * 长时间停机后订单会永远占用库存。这里每 10 分钟扫描一次超时未支付订单，
     * 执行与 OrderUnpaidTask 相同的取消逻辑（乐观锁 + 回补库存 + 返还优惠券）。
     * 与内存任务互为幂等：乐观锁保证只有一方更新成功。
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void checkOrderUnpaid() {
        Integer unpaidMinutes = SystemConfig.getOrderUnpaid();
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(unpaidMinutes);
        List<LeomallOrder> orderList = orderService.queryUnpaid(unpaidMinutes);
        for (LeomallOrder order : orderList) {
            if (order.getAddTime() == null || order.getAddTime().isAfter(deadline)) {
                continue;
            }
            if (!OrderUtil.isCreateStatus(order)) {
                continue;
            }

            order.setOrderStatus(OrderUtil.STATUS_AUTO_CANCEL);
            order.setEndTime(LocalDateTime.now());
            if (orderService.updateWithOptimisticLocker(order) == 0) {
                logger.info("订单 ID=" + order.getId() + " 已被其他任务处理，放弃超时取消");
                continue;
            }

            // 回补库存
            List<LeomallOrderGoods> orderGoodsList = orderGoodsService.queryByOid(order.getId());
            for (LeomallOrderGoods orderGoods : orderGoodsList) {
                if (productService.addStock(orderGoods.getProductId(), orderGoods.getNumber()) == 0) {
                    logger.error("订单 ID=" + order.getId() + " 超时取消回补库存失败 productId=" + orderGoods.getProductId());
                }
            }

            // 返还优惠券
            List<LeomallCouponUser> couponUsers = couponUserService.findByOid(order.getId());
            for (LeomallCouponUser couponUser : couponUsers) {
                couponUser.setStatus(CouponUserConstant.STATUS_USABLE);
                couponUser.setUpdateTime(LocalDateTime.now());
                couponUserService.update(couponUser);
            }

            logger.info("订单 ID=" + order.getId() + " 超时未支付，已兜底取消");
        }
    }

    /**
     * 自动确认订单
     * <p>
     * 定时检查订单未确认情况，如果超时 LEOMALL_ORDER_UNCONFIRM 天则自动确认订单
     * 定时时间是每天凌晨3点。
     * <p>
     * TODO
     * 注意，因为是相隔一天检查，因此导致订单真正超时时间是 [LEOMALL_ORDER_UNCONFIRM, 1 + LEOMALL_ORDER_UNCONFIRM]
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void checkOrderUnconfirm() {
        logger.info("系统开启定时任务检查订单是否已经超期自动确认收货");

        List<LeomallOrder> orderList = orderService.queryUnconfirm(SystemConfig.getOrderUnconfirm());
        for (LeomallOrder order : orderList) {

            // 设置订单已取消状态
            order.setOrderStatus(OrderUtil.STATUS_AUTO_CONFIRM);
            order.setConfirmTime(LocalDateTime.now());
            if (orderService.updateWithOptimisticLocker(order) == 0) {
                logger.info("订单 ID=" + order.getId() + " 数据已经更新，放弃自动确认收货");
            } else {
                logger.info("订单 ID=" + order.getId() + " 已经超期自动确认收货");
            }
        }

        logger.info("系统结束定时任务检查订单是否已经超期自动确认收货");
    }

    /**
     * 可评价订单商品超期
     * <p>
     * 定时检查订单商品评价情况，如果确认商品超时 LEOMALL_ORDER_COMMENT 天则取消可评价状态
     * 定时时间是每天凌晨4点。
     * <p>
     * TODO
     * 注意，因为是相隔一天检查，因此导致订单真正超时时间是 [LEOMALL_ORDER_COMMENT, 1 + LEOMALL_ORDER_COMMENT]
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void checkOrderComment() {
        logger.info("系统开启任务检查订单是否已经超期未评价");

        List<LeomallOrder> orderList = orderService.queryComment(SystemConfig.getOrderComment());
        for (LeomallOrder order : orderList) {
            order.setComments((short) 0);
            orderService.updateWithOptimisticLocker(order);

            List<LeomallOrderGoods> orderGoodsList = orderGoodsService.queryByOid(order.getId());
            for (LeomallOrderGoods orderGoods : orderGoodsList) {
                orderGoods.setComment(-1);
                orderGoodsService.updateById(orderGoods);
            }
        }

        logger.info("系统结束任务检查订单是否已经超期未评价");
    }
}
