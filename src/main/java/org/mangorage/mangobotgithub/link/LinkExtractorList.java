package org.mangorage.mangobotgithub.link;

import org.mangorage.mangobotgithub.link.extractors.GnomebotExtractor;

import java.util.List;

public final class LinkExtractorList {
    public static final LinkExtractorList LIST = new LinkExtractorList(
            List.of(
                    new GnomebotExtractor()
            )
    );
    private final List<LinkExtractor> list;
    LinkExtractorList(List<LinkExtractor> list) {
        this.list = list;
    }

    public String fetch(String url) {
        for (LinkExtractor linkExtractor : list) {
            var result = linkExtractor.fetch(url);
            if (result.isPresent())
                return result.get();
        }
        return null;
    }
}
