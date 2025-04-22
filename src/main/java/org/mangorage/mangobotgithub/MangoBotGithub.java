package org.mangorage.mangobotgithub;


import org.mangorage.commonutils.config.Config;
import org.mangorage.commonutils.config.ConfigSetting;
import org.mangorage.commonutils.config.ISetting;
import org.mangorage.mangobotcore.plugin.api.MangoBotPlugin;
import org.mangorage.mangobotcore.plugin.api.Plugin;
import org.mangorage.mangobotcore.plugin.api.PluginManager;
import org.mangorage.mangobotgithub.core.GHIssueStatus;
import org.mangorage.mangobotgithub.core.GHPRStatus;
import org.mangorage.mangobotgithub.core.GuildConfig;
import org.mangorage.mangobotgithub.core.IssueScanCommand;
import org.mangorage.mangobotgithub.core.PRScanCommand;
import org.mangorage.mangobotgithub.core.PasteRequestModule;
import org.mangorage.mangobotplugin.entrypoint.MangoBot;

import java.nio.file.Path;

@MangoBotPlugin(id = MangoBotGithub.ID)
public final class MangoBotGithub implements Plugin {
    public static final String ID = "mangobotgithub";


    // Where we create our "config"
    public final static Config CONFIG = new Config(Path.of("plugins/%s/.env".formatted(MangoBotGithub.ID)));

    public static final ISetting<String> GITHUB_TOKEN = ConfigSetting.create(CONFIG, "PASTE_TOKEN", "empty");
    public static final ISetting<String> GITHUB_USERNAME = ConfigSetting.create(CONFIG, "GITHUB_USERNAME", "RealMangoRage");


    public MangoBotGithub() {
        PasteRequestModule.register();
    }


    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void load() {
        GuildConfig.loadServerConfigs();

        var parent = PluginManager.getInstance().getPlugin("mangobot").getInstance(MangoBot.class);
        var cmdRegistry = parent.getCommandManager();
        cmdRegistry.register(new PRScanCommand(parent));
        cmdRegistry.register(new IssueScanCommand(parent));
        new GHPRStatus(parent);
        new GHIssueStatus(parent);
    }
}
