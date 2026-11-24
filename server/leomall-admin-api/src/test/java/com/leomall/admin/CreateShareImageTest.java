package com.leomall.admin;

import org.junit.jupiter.api.Test;
import com.leomall.core.qcode.QCodeService;
import com.leomall.db.domain.LeomallGoods;
import com.leomall.db.service.LeomallGoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

@WebAppConfiguration
@SpringBootTest
public class CreateShareImageTest {
    @Autowired
    QCodeService qCodeService;
    @Autowired
    LeomallGoodsService leomallGoodsService;

    @Test
    public void test() {
        LeomallGoods good = leomallGoodsService.findById(1181010);
        qCodeService.createGoodShareImage(good.getId().toString(), good.getPicUrl(), good.getName());
    }
}
