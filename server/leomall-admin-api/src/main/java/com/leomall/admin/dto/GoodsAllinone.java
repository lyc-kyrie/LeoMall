package com.leomall.admin.dto;

import com.leomall.db.domain.LeomallGoods;
import com.leomall.db.domain.LeomallGoodsAttribute;
import com.leomall.db.domain.LeomallGoodsProduct;
import com.leomall.db.domain.LeomallGoodsSpecification;

public class GoodsAllinone {
    LeomallGoods goods;
    LeomallGoodsSpecification[] specifications;
    LeomallGoodsAttribute[] attributes;
    LeomallGoodsProduct[] products;

    public LeomallGoods getGoods() {
        return goods;
    }

    public void setGoods(LeomallGoods goods) {
        this.goods = goods;
    }

    public LeomallGoodsProduct[] getProducts() {
        return products;
    }

    public void setProducts(LeomallGoodsProduct[] products) {
        this.products = products;
    }

    public LeomallGoodsSpecification[] getSpecifications() {
        return specifications;
    }

    public void setSpecifications(LeomallGoodsSpecification[] specifications) {
        this.specifications = specifications;
    }

    public LeomallGoodsAttribute[] getAttributes() {
        return attributes;
    }

    public void setAttributes(LeomallGoodsAttribute[] attributes) {
        this.attributes = attributes;
    }

}
