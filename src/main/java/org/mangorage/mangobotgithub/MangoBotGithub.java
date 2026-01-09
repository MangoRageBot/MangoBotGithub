package org.mangorage.mangobotgithub;


import org.mangorage.mangobotcore.config.api.ConfigTypes;
import org.mangorage.mangobotcore.config.api.IConfig;
import org.mangorage.mangobotcore.config.api.IConfigSetting;
import org.mangorage.mangobotcore.plugin.api.MangoBotPlugin;
import org.mangorage.mangobotcore.plugin.api.Plugin;
import org.mangorage.mangobotgithub.core.PasteRequestModule;

import java.nio.file.Path;
import java.util.UUID;

@MangoBotPlugin(id = MangoBotGithub.ID)
public final class MangoBotGithub implements Plugin {
    public static final String ID = "mangobotgithub";

    public final static IConfig CONFIG = IConfig.create(Path.of("plugins/%s/.env".formatted(MangoBotGithub.ID)));
    public static final IConfigSetting<String> GITHUB_TOKEN = IConfigSetting.create(CONFIG, "PASTE_TOKEN", ConfigTypes.STRING, "empty");
    public static final IConfigSetting<String> MANGOBOT_UPLOAD_TOKEN = IConfigSetting.create(CONFIG, "MANGOBOT_UPLOAD_TOKEN", ConfigTypes.STRING, UUID.randomUUID().toString()); // Generates one by default.

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
