package org.mangorage.mangobotgithub.core.integration;

import net.dv8tion.jda.api.entities.Message;
import org.mangorage.mangobotapi.core.plugin.PluginManager;
import org.mangorage.mangobotsite.MangoBotSite;
import org.mangorage.mangobotsite.website.file.FileStream;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public final class MangoBotSiteIntegration {
    private static final String ID = "5473b5bf-044b-4a57-a78d-be61289e4266";

    public static String handleUpload(List<Message.Attachment> attachments) throws IOException {
        return PluginManager.getPlugin("mangobotsite", MangoBotSite.class)
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
