package org.mangorage.mangobotgithub.core;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.eclipse.egit.github.core.Gist;
import org.eclipse.egit.github.core.GistFile;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.GistService;
import org.mangorage.mangobotcore.api.jda.event.v1.DiscordMessageReactionAddEvent;
import org.mangorage.mangobotcore.api.jda.event.v1.DiscordMessageReceivedEvent;
import org.mangorage.mangobotcore.api.plugin.v1.PluginManager;
import org.mangorage.mangobotcore.api.util.misc.LazyReference;
import org.mangorage.mangobotcore.api.util.misc.TaskScheduler;
import org.mangorage.mangobotgithub.MangoBotGithub;
import org.mangorage.mangobotgithub.core.integration.MangoBotSiteIntegration;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public final class PasteRequestModule {

    static final LazyReference<GitHubClient> GITHUB_CLIENT = LazyReference.create(() -> new GitHubClient().setOAuth2Token(MangoBotGithub.GITHUB_TOKEN.get()));

    private static final List<String> GUILDS = List.of(
            "1129059589325852724", // Forge Discord
            "834300742864601088",
            "1179586337431633991",
            "716249661798612992" // BenBenLaw Server
    );

    private static final List<String> VALID_CONTENT_TYPES = List.of(
            "application/json",
            "text"
    );

    private static final Emoji CREATE_GISTS = Emoji.fromUnicode("\uD83D\uDCCB");

    public static void register() {
        DiscordMessageReceivedEvent.BUS.addListener(PasteRequestModule::onMessage);
        DiscordMessageReactionAddEvent.BUS.addListener(PasteRequestModule::onReact);
    }

    private static byte[] getData(InputStream stream) {
        try (var is = stream) {
            return is.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    private static String getFileName(Message.Attachment attachment, int count) {
        var fileName = attachment.getFileName();
        var ext = ".%s".formatted(attachment.getFileExtension());
        if (ext == null) return attachment.getFileName();
        var fileNameNoExt = fileName.substring(0, fileName.length() - ext.length());
        return "%s_%s%s".formatted(fileNameNoExt, count, ext);
    }

    public static void createGists(Message msg, User requester) {
        TaskScheduler.getExecutor().execute(() -> {
            if (!msg.isFromGuild()) return;
            if (!GUILDS.contains(msg.getGuildId())) {
                msg.reply("Your server is not on the allowlist for Gist Paste. Please contact the server admin if you use wish to use this functionality.").mentionRepliedUser(false).queue();
                return;
            }

            var attachments = msg.getAttachments();
            if (attachments.isEmpty()) return;

            GitHubClient CLIENT = GITHUB_CLIENT.get();
            GistService service = new GistService(CLIENT);
            AtomicInteger count = new AtomicInteger(1);

            Gist gist = new Gist();
            gist.setPublic(false);
            gist.setDescription("Automatically made from MangoBot.");

            HashMap<String, GistFile> FILES = new HashMap<>();

            String id = null;
            if (PluginManager.getInstance().getPlugin("mangobotsite") != null) {
                try {
                    id = MangoBotSiteIntegration.handleUpload(attachments);
                } catch (IOException ignored) {}
            }

            attachments.forEach(attachment -> {
                try {

                    byte[] bytes = getData(attachment.getProxy().download().get());
                    if (bytes == null) return;
                    String content = new String(bytes, StandardCharsets.UTF_8);
                    var fileName = getFileName(attachment, count.getAndAdd(1));

                    var gistFile = new GistFile();
                    gistFile.setContent(content);
                    gistFile.setFilename(fileName);

                    FILES.put(fileName, gistFile);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });

            gist.setFiles(FILES);

            if (FILES.isEmpty() && id != null) {
                msg.reply(("Upload -> [[mango](https://mangobot.mangorage.org/file?id=%s)]".formatted(id))).setSuppressEmbeds(true).mentionRepliedUser(false).queue();
            } else {
                try {
                    var remote = service.createGist(gist);
                    StringBuilder result = new StringBuilder();
                    result.append("Gist -> [[gist](%s)]".formatted(remote.getHtmlUrl()));

                    if (id != null) {
                        result.append(" [[mango](https://mangobot.mangorage.org/file?id=%s)]".formatted(id));
                    }

                    remote.getFiles().forEach((key, file) -> {
                        result.append(" [[raw %s](%s)]".formatted(file.getFilename(), file.getRawUrl()));
                    });

                    msg.reply(result).setSuppressEmbeds(true).mentionRepliedUser(false).queue();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        });
    }

    public static void onMessage(DiscordMessageReceivedEvent event) {
        var discordEvent = event.getDiscordEvent();
        var message = discordEvent.getMessage();

        if (message.getAuthor().isBot()) return;
        if (message.getAuthor().isSystem()) return;

        if (!message.getAttachments().isEmpty()) {
            chk: for (Message.Attachment attachment : message.getAttachments()) {
                for (String validContentType : VALID_CONTENT_TYPES) {
                    var contentType = attachment.getContentType();
                    if (contentType != null && contentType.contains(validContentType)) {
                        message.addReaction(CREATE_GISTS).queue();
                        break chk;
                    }
                }
            }
        }
    }

    public static void onReact(DiscordMessageReactionAddEvent event) {
        var dEvent = event.getDiscordEvent();

        if (!dEvent.isFromGuild()) return;
        if (dEvent.getUser() == null) return;
        if (dEvent.getUser().isBot()) return;

        dEvent.retrieveMessage().queue(a -> {
            a.retrieveReactionUsers(CREATE_GISTS).queue(b -> {
                b.stream().filter(user -> !user.isBot()).findFirst().ifPresent(c -> {
                    a.clearReactions(CREATE_GISTS).queue();
                    createGists(a, dEvent.getUser());
                });
            });
        });

    }
}
