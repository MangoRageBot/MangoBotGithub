package org.mangorage.mangobotgithub;


import org.mangorage.commonutils.config.Config;
import org.mangorage.commonutils.config.ConfigSetting;
import org.mangorage.commonutils.config.ISetting;
import org.mangorage.mangobotcore.plugin.api.MangoBotPlugin;
import org.mangorage.mangobotcore.plugin.api.Plugin;
import org.mangorage.mangobotgithub.core.PasteRequestModule;

import java.nio.file.Path;
import java.util.UUID;

@MangoBotPlugin(id = MangoBotGithub.ID)
public final class MangoBotGithub implements Plugin {
    public static final String ID = "mangobotgithub";

    public final static Config CONFIG = new Config(Path.of("plugins/%s/.env".formatted(MangoBotGithub.ID)));
    public static final ISetting<String> GITHUB_TOKEN = ConfigSetting.create(CONFIG, "PASTE_TOKEN", "empty");
    public static final ISetting<String> MANGOBOT_UPLOAD_TOKEN = ConfigSetting.create(CONFIG, "MANGOBOT_UPLOAD_TOKEN", UUID.randomUUID().toString()); // Generates one by default.

    public MangoBotGithub() {

    }


    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void load() {
        PasteRequestModule.register();
    }
}
