package com.leomall.core.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** P2-9 回归：富文本白名单消毒 */
public class RichTextUtilTest {

    @Test
    public void removesScriptAndEvents() {
        String dirty = "<p onclick=\"alert(1)\">hi</p><script>alert(1)</script>";
        String clean = RichTextUtil.clean(dirty);
        Assertions.assertFalse(clean.contains("script"));
        Assertions.assertFalse(clean.contains("onclick"));
        Assertions.assertTrue(clean.contains("hi"));
    }

    @Test
    public void removesIframeAndJavascriptProtocol() {
        String dirty = "<iframe src=\"http://evil\"></iframe><a href=\"javascript:alert(1)\">x</a>";
        String clean = RichTextUtil.clean(dirty);
        Assertions.assertFalse(clean.contains("iframe"));
        Assertions.assertFalse(clean.toLowerCase().contains("javascript:"));
    }

    @Test
    public void keepsSafeMarkupAndStyle() {
        String safe = "<p style=\"color:red\" class=\"a\"><strong>bold</strong><img src=\"https://x/y.png\" width=\"10\"></p>";
        String clean = RichTextUtil.clean(safe);
        Assertions.assertTrue(clean.contains("<strong>"));
        Assertions.assertTrue(clean.contains("color:red"));
        Assertions.assertTrue(clean.contains("https://x/y.png"));
    }

    @Test
    public void nullAndEmptyPassThrough() {
        Assertions.assertNull(RichTextUtil.clean(null));
        Assertions.assertEquals("", RichTextUtil.clean(""));
    }
}