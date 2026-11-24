package com.leomall.core.util;

import org.junit.Assert;
import org.junit.Test;

/** P2-9 回归：富文本白名单消毒 */
public class RichTextUtilTest {

    @Test
    public void removesScriptAndEvents() {
        String dirty = "<p onclick=\"alert(1)\">hi</p><script>alert(1)</script>";
        String clean = RichTextUtil.clean(dirty);
        Assert.assertFalse(clean.contains("script"));
        Assert.assertFalse(clean.contains("onclick"));
        Assert.assertTrue(clean.contains("hi"));
    }

    @Test
    public void removesIframeAndJavascriptProtocol() {
        String dirty = "<iframe src=\"http://evil\"></iframe><a href=\"javascript:alert(1)\">x</a>";
        String clean = RichTextUtil.clean(dirty);
        Assert.assertFalse(clean.contains("iframe"));
        Assert.assertFalse(clean.toLowerCase().contains("javascript:"));
    }

    @Test
    public void keepsSafeMarkupAndStyle() {
        String safe = "<p style=\"color:red\" class=\"a\"><strong>bold</strong><img src=\"https://x/y.png\" width=\"10\"></p>";
        String clean = RichTextUtil.clean(safe);
        Assert.assertTrue(clean.contains("<strong>"));
        Assert.assertTrue(clean.contains("color:red"));
        Assert.assertTrue(clean.contains("https://x/y.png"));
    }

    @Test
    public void nullAndEmptyPassThrough() {
        Assert.assertNull(RichTextUtil.clean(null));
        Assert.assertEquals("", RichTextUtil.clean(""));
    }
}