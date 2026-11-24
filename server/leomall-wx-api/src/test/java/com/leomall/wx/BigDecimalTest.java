package com.leomall.wx;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/** P3-2：改为纯单元测试（原 SpringJUnit4ClassRunner 无上下文配置必挂），补断言 */
public class BigDecimalTest {

    @Test
    public void test() {
        BigDecimal a = new BigDecimal(0);
        BigDecimal b = new BigDecimal(1);
        BigDecimal c = a.subtract(b);
        BigDecimal d = c.max(new BigDecimal(0));

        Assertions.assertEquals(new BigDecimal(-1), c);
        Assertions.assertEquals(0, d.compareTo(BigDecimal.ZERO));
        // P2-1 回归：金额比较必须用 compareTo，equals 受 scale 影响
        Assertions.assertNotEquals(new BigDecimal("1.0"), new BigDecimal("1.00"));
        Assertions.assertEquals(0, new BigDecimal("1.0").compareTo(new BigDecimal("1.00")));
    }
}