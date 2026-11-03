package com.sports.common.util;

import java.util.regex.Pattern;

/**
 * 富文本安全过滤工具
 * 采用白名单/黑名单组合的方式去除可执行脚本与事件属性，防止存储型 XSS。
 *
 * @author sports
 */
public final class HtmlSanitizer {

    /** 危险标签（含内容） */
    private static final Pattern DANGEROUS_TAGS = Pattern.compile(
            "(?is)<\\s*(script|style|iframe|object|embed|link|meta|base|form)\\b[^>]*>.*?<\\s*/\\s*\\1\\s*>");
    /** 自闭合危险标签 */
    private static final Pattern DANGEROUS_SINGLE_TAGS = Pattern.compile(
            "(?is)<\\s*(script|style|iframe|object|embed|link|meta|base|form)\\b[^>]*/?>");
    /** on* 事件属性 */
    private static final Pattern EVENT_ATTR = Pattern.compile(
            "(?is)\\s+on[a-z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)");
    /** javascript: / vbscript: 协议 */
    private static final Pattern DANGEROUS_PROTOCOL = Pattern.compile(
            "(?is)(href|src|xlink:href)\\s*=\\s*(\"|')?\\s*(javascript|vbscript|data:text/html)[^\"'>\\s]*(\"|')?");

    private HtmlSanitizer() {
    }

    /**
     * 过滤富文本中的可执行内容
     *
     * @param html 原始 HTML
     * @return 过滤后的 HTML
     */
    public static String clean(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        String result = DANGEROUS_TAGS.matcher(html).replaceAll("");
        result = DANGEROUS_SINGLE_TAGS.matcher(result).replaceAll("");
        result = EVENT_ATTR.matcher(result).replaceAll("");
        result = DANGEROUS_PROTOCOL.matcher(result).replaceAll("$1=\"#\"");
        return result;
    }
}
