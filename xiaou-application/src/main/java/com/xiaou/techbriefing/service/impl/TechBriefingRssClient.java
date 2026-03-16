package com.xiaou.techbriefing.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.XmlUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.techbriefing.domain.TechBriefingSource;
import com.xiaou.techbriefing.dto.model.TechBriefingFeedItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * RSS 抓取客户端
 */
@Slf4j
@Component
public class TechBriefingRssClient {

    public List<TechBriefingFeedItem> fetch(TechBriefingSource source) {
        if (source == null || StrUtil.isBlank(source.getRssUrl())) {
            return List.of();
        }
        try (HttpResponse response = HttpRequest.get(source.getRssUrl())
                .timeout(15000)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36")
                .execute()) {
            if (!response.isOk()) {
                throw new BusinessException("抓取 RSS 失败，状态码：" + response.getStatus());
            }
            String xml = response.body();
            if (StrUtil.isBlank(xml)) {
                return List.of();
            }
            Document document = XmlUtil.readXML(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            NodeList itemNodes = document.getElementsByTagName("item");
            List<TechBriefingFeedItem> items = new ArrayList<>();
            for (int i = 0; i < itemNodes.getLength(); i++) {
                Node node = itemNodes.item(i);
                if (!(node instanceof Element element)) {
                    continue;
                }
                items.add(new TechBriefingFeedItem()
                        .setTitle(readText(element, "title"))
                        .setLink(normalizeUrl(readText(element, "link")))
                        .setSummary(cleanHtml(readText(element, "description")))
                        .setContent(extractContent(element))
                        .setImageUrl(readText(element, "image"))
                        .setPublishTime(parseDate(readText(element, "pubDate")))
                        .setCategories(extractCategories(element)));
            }
            return items;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("抓取 RSS 失败: source={}", source.getSourceName(), ex);
            throw new BusinessException("抓取 RSS 失败：" + source.getSourceName());
        }
    }

    private String extractContent(Element element) {
        String content = readText(element, "content:encoded");
        if (StrUtil.isBlank(content)) {
            content = readText(element, "content");
        }
        if (StrUtil.isBlank(content)) {
            content = readText(element, "description");
        }
        return cleanHtml(content);
    }

    private List<String> extractCategories(Element element) {
        NodeList categoryNodes = element.getElementsByTagName("category");
        Set<String> categories = new LinkedHashSet<>();
        for (int i = 0; i < categoryNodes.getLength(); i++) {
            String text = StrUtil.trim(categoryNodes.item(i).getTextContent());
            if (StrUtil.isNotBlank(text)) {
                categories.add(text);
            }
        }
        return new ArrayList<>(categories);
    }

    private LocalDateTime parseDate(String pubDate) {
        if (StrUtil.isBlank(pubDate)) {
            return LocalDateTime.now();
        }
        try {
            return ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .withZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (Exception ignored) {
            return LocalDateTime.now();
        }
    }

    private String readText(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes == null || nodes.getLength() == 0) {
            return null;
        }
        return nodes.item(0).getTextContent();
    }

    private String cleanHtml(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        String cleaned = value.replaceAll("<[^>]+>", " ");
        cleaned = cleaned.replace("&nbsp;", " ").replace("&amp;", "&");
        return StrUtil.trim(cleaned.replaceAll("\\s+", " "));
    }

    private String normalizeUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return url;
        }
        return url.replace("&amp;", "&");
    }
}
