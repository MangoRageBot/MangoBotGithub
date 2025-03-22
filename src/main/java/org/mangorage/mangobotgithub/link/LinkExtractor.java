package org.mangorage.mangobotgithub.link;

import java.util.Optional;

public interface LinkExtractor {
    Optional<String> fetch(String url);
}
