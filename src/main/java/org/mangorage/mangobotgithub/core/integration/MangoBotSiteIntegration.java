package org.mangorage.mangobotgithub.core.integration;


import net.dv8tion.jda.api.entities.Message;
import org.mangorage.mangobotcore.api.plugin.v1.PluginManager;
import org.mangorage.mangobotgithub.MangoBotGithub;
import org.mangorage.mangobotsite.MangoBotSite;
import org.mangorage.mangobotsite.website.file.FileStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public final class MangoBotSiteIntegration {
    private static final String ID = MangoBotGithub.MANGOBOT_UPLOAD_TOKEN.get();

    public static String handleUpload(List<Message.Attachment> attachments) throws IOException {
        return PluginManager.getInstance().getPlugin("mangobotsite")
                .getInstance(MangoBotSite.class)
                .getFileUploadManager()
                .createUpload(
                        attachments.stream()
                                .map(attachment -> new FileStream(attachment.getFileName(), () -> {
                                    try {
                                        return attachment.getProxy().download().get();
                                    } catch (InterruptedException | ExecutionException e) {
                                        throw new IOException(e);
                                    }
                                })).toList(),
                        ID
                );
    }
}
