module org.mangorage.mangobotgithub {
    requires okhttp3;
    requires org.eclipse.egit.github.core;
    requires org.kohsuke.github.api;
    requires org.mangorage.mangobotplugin;
    requires org.mangorage.mangobotcore;
    requires net.dv8tion.jda;
    requires net.minecraftforge.eventbus;

    exports org.mangorage.mangobotgithub;

    provides org.mangorage.mangobotcore.api.plugin.v1.Plugin with org.mangorage.mangobotgithub.MangoBotGithub;
    uses org.mangorage.mangobotcore.api.plugin.v1.Plugin;
}