package org.mangorage.mangobotgithub.core;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import org.eclipse.egit.github.core.Gist;
import org.eclipse.egit.github.core.GistFile;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.GistService;
import org.mangorage.basicutils.TaskScheduler;
import org.mangorage.basicutils.misc.LazyReference;
import org.mangorage.eventbus.interfaces.IEventBus;
import org.mangorage.eventbus.interfaces.IEventType;
import org.mangorage.mangobot.modules.logs.LogAnalyser;
import org.mangorage.mangobot.modules.logs.LogAnalyserModule;
import org.mangorage.mangobotapi.core.events.DiscordEvent;
import org.mangorage.mangobotapi.core.plugin.PluginManager;
import org.mangorage.mangobotgithub.MangoBotGithub;
import org.mangorage.mangobotgithub.core.integration.MangoBotSiteIntegration;
import org.mangorage.mangobotgithub.link.LinkExtractorList;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PasteRequestModule {
    private static final Pattern urlPattern = Pattern.compile("(https?://\\S+)");

    public static final LogAnalyser analyser = LogAnalyserModule.MAIN;

    static final LazyReference<GitHubClient> GITHUB_CLIENT = LazyReference.create(() -> new GitHubClient().setOAuth2Token(MangoBotGithub.GITHUB_TOKEN.get()));

    private static final List<String> GUILDS = List.of(
            "1129059589325852724", // Forge Discord
            "834300742864601088",
            "1179586337431633991",
            "716249661798612992" // BenBenLaw Server
    );
    private static final Emoji CREATE_GISTS = Emoji.fromUnicode("\uD83D\uDCCB");
    private static final Emoji ANALYZE = Emoji.fromUnicode("\uD83E\uDDD0");

    public static void register(IEventBus<IEventType.INormalBusEvent> bus) {
        bus.addGenericListener(10, MessageReceivedEvent.class, DiscordEvent.class, PasteRequestModule::onMessage);
        bus.addGenericListener(10, MessageReactionAddEvent.class, DiscordEvent.class, PasteRequestModule::onReact);
    }

    private static byte[] getData(InputStream stream) {
        try {
            byte[] data = stream.readAllBytes();
            stream.close();
            return data;
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

    private static double calculatePrintableCharacterConfidence(String input) {
        // Count the number of printable characters
        long printableCount = input.codePoints().filter(codePoint -> codePoint >= 0x20 && codePoint <= 0x7E).count();

        // Calculate the ratio of printable characters to total characters
        double confidence = (double) printableCount / input.length();

        return confidence;
    }

    private static boolean containsPrintableCharacters(String input) {
        // Use a regular expression to match all printable characters, including colon and semicolon
        return calculatePrintableCharacterConfidence(input) > 0.6;
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
            if (PluginManager.isLoaded("mangobotsite")) {
                try {
                    id = MangoBotSiteIntegration.handleUpload(attachments);
                } catch (IOException ignored) {}
            }

            attachments.forEach(attachment -> {
                try {

                    byte[] bytes = getData(attachment.getProxy().download().get());
                    if (bytes == null) return;
                    String content = new String(bytes, StandardCharsets.UTF_8);
                    if (!containsPrintableCharacters(content)) return;
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

    public static List<String> extractUrls(String text) {
        List<String> urls = new ArrayList<>();
        Matcher matcher = urlPattern.matcher(text);
        while (matcher.find()) {
            urls.add(matcher.group(1));
        }
        return urls;
    }

    public static void analyzeLog(Message message) {
        var attachments = message.getAttachments();

        var builder = new StringBuilder();
        analyser.scanMessage(message, builder);


        for (Message.Attachment attachment : attachments) {
            try {
                byte[] bytes = getData(attachment.getProxy().download().get());
                if (bytes == null) continue;
                String content = new String(bytes, StandardCharsets.UTF_8);
                if (containsPrintableCharacters(content)) {
                    analyser.readLog(builder, content);
                    break;
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        // Handle Links in the actual message
        for (String extractUrl : extractUrls(message.getContentRaw())) {
            System.out.println(extractUrl);
            var log = LinkExtractorList.LIST.fetch(extractUrl);
            if (log != null) {
                analyser.readLog(builder, log);
                System.out.println("Read log");
            }
        }

        System.out.println(builder);

        builder.append("Testing the Analyzer");

        if (!builder.isEmpty()) {;
            String id = null;
            if (PluginManager.isLoaded("mangobotsite")) {
                System.out.println("Uploaded to MangoBot Site");
                try {
                    id = MangoBotSiteIntegration.handleLogResult(builder);
                    if (id != null) {
                        message.reply("[[Log Analyzer](https://mangobot.mangorage.org/file?id=%s)]".formatted(id)).setSuppressEmbeds(true).mentionRepliedUser(false).queue();
                    }
                } catch (IOException ignored) {
                    ignored.printStackTrace();
                }
            }
        }


    }

    public static void onMessage(DiscordEvent<MessageReceivedEvent> event) {
        var discordEvent = event.getInstance();
        var message = discordEvent.getMessage();
        var analyze = false;

        if (message.getAuthor().isBot()) return;
        if (message.getAuthor().isSystem()) return;


        if (message.getContentRaw().contains("https://")) {
            analyze = true;
        }

        if (!message.getAttachments().isEmpty()) {
            message.addReaction(CREATE_GISTS).queue();
            analyze = true;
        }

        if (analyze)
            message.addReaction(ANALYZE).queue();
    }

    public static void onReact(DiscordEvent<MessageReactionAddEvent> event) {
        var dEvent = event.getInstance();

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

            a.retrieveReactionUsers(ANALYZE).queue(b -> {
                b.stream().filter(user -> !user.isBot()).findFirst().ifPresent(c -> {
                    a.clearReactions(ANALYZE).queue();
                    analyzeLog(a);
                });
            });

        });

    }
}
