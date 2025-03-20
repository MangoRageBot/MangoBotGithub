package org.mangorage.mangobotgithub;

import org.mangorage.basicutils.config.Config;
import org.mangorage.basicutils.config.ConfigSetting;
import org.mangorage.basicutils.config.ISetting;
import org.mangorage.mangobot.MangoBotPlugin;
import org.mangorage.mangobotapi.core.events.StartupEvent;
import org.mangorage.mangobotapi.core.plugin.AbstractPlugin;
import org.mangorage.mangobotapi.core.plugin.PluginManager;
import org.mangorage.mangobotapi.core.plugin.impl.Plugin;
import org.mangorage.mangobotgithub.core.GHIssueStatus;
import org.mangorage.mangobotgithub.core.GHPRStatus;
import org.mangorage.mangobotgithub.core.GuildConfig;
import org.mangorage.mangobotgithub.core.IssueScanCommand;
import org.mangorage.mangobotgithub.core.PRScanCommand;
import org.mangorage.mangobotgithub.core.PasteRequestModule;

import java.nio.file.Path;

@Plugin(id = MangoBotGithub.ID)
public final class MangoBotGithub extends AbstractPlugin {
    public static final String ID = "mangobotgithub";


    // Where we create our "config"
    public final static Config CONFIG = new Config(Path.of("plugins/%s/.env".formatted(MangoBotGithub.ID)));

    public static final ISetting<String> GITHUB_TOKEN = ConfigSetting.create(CONFIG, "PASTE_TOKEN", "empty");
    public static final ISetting<String> GITHUB_USERNAME = ConfigSetting.create(CONFIG, "GITHUB_USERNAME", "RealMangoRage");
    public static final ISetting<String> CHAT_AI_TOKEN = ConfigSetting.create(CONFIG, "AI_TOKEN", "empty");


    private final MangoBotPlugin parent;

    public MangoBotGithub() {
        var pl = PluginManager.getPlugin("mangobot", MangoBotPlugin.class);
        this.parent = pl;
        PasteRequestModule.register(pl.getPluginBus());


        pl.getPluginBus().addListener(0, StartupEvent.class, this::onRegistration);
    }

    @Override
    protected void init() {

    }

    public void onRegistration(StartupEvent event) {
        if (event.phase() == StartupEvent.Phase.REGISTRATION) {
            GuildConfig.loadServerConfigs();


            var cmdRegistry = parent.getCommandRegistry();
            cmdRegistry.addBasicCommand(new PRScanCommand(parent));
            cmdRegistry.addBasicCommand(new IssueScanCommand(parent));
            cmdRegistry.addBasicCommand(new AICommand());

            new GHPRStatus(parent);
            new GHIssueStatus(parent);
        }
    }
}
