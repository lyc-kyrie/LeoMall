package com.leomall.core.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * P2-9：富文本服务端白名单消毒。
 *
 * 商品详情 / 专题 / 公告等由管理端录入的 HTML 会经前端 v-html 直达用户端，
 * 入库前统一清洗：移除 script/iframe/事件属性/javascript: 协议等，
 * 仅保留排版所需标签与 style/class，防止存储型 XSS。
 */
public class RichTextUtil {

    private static final Safelist SAFELIST = Safelist.relaxed()
            .addAttributes(":all", "style", "class")
            .addAttributes("img", "width", "height")
            .addProtocols("img", "src", "http", "https");

    private static final Document.OutputSettings OUTPUT_SETTINGS =
            new Document.OutputSettings().prettyPrint(false);

    public static String clean(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        return Jsoup.clean(html, "", SAFELIST, OUTPUT_SETTINGS);
    }
}