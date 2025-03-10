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
import org.mangorage.mangobot.modules.logs.BrokenDrivers;
import org.mangorage.mangobot.modules.logs.EarlyWindow;
import org.mangorage.mangobot.modules.logs.Java22;
import org.mangorage.mangobot.modules.logs.LogAnalyser;
import org.mangorage.mangobot.modules.logs.MissingDeps;
import org.mangorage.mangobotapi.core.events.DiscordEvent;
import org.mangorage.mangobotapi.core.plugin.PluginManager;
import org.mangorage.mangobotgithub.MangoBotGithub;
import org.mangorage.mangobotgithub.core.integration.MangoBotSiteIntegration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PasteRequestModule {
    public static final LogAnalyser analyser = LogAnalyser.of(
            new BrokenDrivers(),
            new EarlyWindow(),
            new Java22(),
            new MissingDeps(),
            LogAnalyser.createModule(
                    (s, m) -> {
                        m.reply("This is a common issue on Modrinth Theseus. Modrinth's launcher has been known to be problematic in some cases with Forge. If you need to download a Modrinth format modpack you can use Prism Launcher, GDLauncher, ATLauncher, or others which are far more reliable.").setSuppressEmbeds(true).mentionRepliedUser(true).queue();
                    },
                    List.of(
                            "Invalid registry value type detected for PerfOS counters",
                            "com.modrinth.theseus"
                    )
            ),
            LogAnalyser.createModule(
                    (s, m) -> {
                        m.reply("This issue is in most cases caused by an outdated version of Java with issues with Let's Encrypt SSL. Please Update to a newer build of Java [Guide](https://mikumikudance.jp/index.php?title=Installing_Java_For_Minecraft). It can also be caused by networking issues.").setSuppressEmbeds(true).mentionRepliedUser(true).queue();
                    },
                    List.of(
                            "net.minecraftforge.installertools",
                            "sun.security.validator.PKIXValidator"
                    )
            ),
            LogAnalyser.createModule(
                    (s, m) -> {
                        m.reply("Use Java 8. [Guide](https://mikumikudance.jp/index.php?title=Installing_Java_For_Minecraft).").setSuppressEmbeds(true).mentionRepliedUser(true).queue();
                    },
                    List.of(
                            "jdk.internal.loader.ClassLoaders$AppClassLoader cannot be cast to class java.net.URLClassLoader"
                    )
            ),
            LogAnalyser.createModule(
                    (s, m) -> {
                        m.reply("You are using old Java version. Use Java 17 for 1.17-1.20.4 or Java 21 for 1.20.5+. [Guide](https://mikumikudance.jp/index.php?title=Installing_Java_For_Minecraft).").setSuppressEmbeds(true).mentionRepliedUser(true).queue();
                    },
                    List.of(
                            "Current Java is",
                            "but we require at least"
                    )
            ),
            LogAnalyser.createModule(
                    (s, m) -> {
                        m.reply("You are using old Java version. Use Java 17 for 1.17-1.20.4 or Java 21 for 1.20.5+. [Guide](https://mikumikudance.jp/index.php?title=Installing_Java_For_Minecraft).").setSuppressEmbeds(true).mentionRepliedUser(true).queue();
                    },
                    List.of(
                            "Error: could not open",
                            "user_jvm_args.txt"
                    )
            ),
            LogAnalyser.createModule(
                    (s, m) -> {
                        m.reply("Update FeatureCreep").setSuppressEmbeds(true).mentionRepliedUser(true).queue();
                    },
                    List.of(
                            "Caused by: java.lang.IllegalArgumentException: Missing scheme",
                            "org.jboss.modules"
                    )
            )
    );


    static final LazyReference<GitHubClient> GITHUB_CLIENT = LazyReference.create(() -> new GitHubClient().setOAuth2Token(MangoBotGithub.GITHUB_TOKEN.get()));
    private static final List<String> GUILDS = List.of(
            "1129059589325852724", // Forge Discord
            "834300742864601088",
            "1179586337431633991",
            "716249661798612992" // BenBenLaw Server
    );
    private static final Emoji EMOJI = Emoji.fromUnicode("\uD83D\uDCCB");

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

    public static void onMessage(DiscordEvent<MessageReceivedEvent> event) {
        var dEvent = event.getInstance();
        var message = dEvent.getMessage();
        var attachments = message.getAttachments();
        analyser.scanMessage(message);
        if (!attachments.isEmpty()) {
            TaskScheduler.getExecutor().execute(() -> {
                var suceeeded = new AtomicBoolean(false);
                for (Message.Attachment attachment : attachments) {
                    try {
                        byte[] bytes = getData(attachment.getProxy().download().get());
                        if (bytes == null) continue;
                        String content = new String(bytes, StandardCharsets.UTF_8);
                        if (containsPrintableCharacters(content)) {
                            suceeeded.set(true);
                            analyser.readLog(message, content);
                            break;
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }

                if (suceeeded.get()) message.addReaction(EMOJI).queue();
            });
        }
    }

    public static void onReact(DiscordEvent<MessageReactionAddEvent> event) {
        var dEvent = event.getInstance();

        if (!dEvent.isFromGuild()) return;
        if (dEvent.getUser() == null) return;
        if (dEvent.getUser().isBot()) return;

        dEvent.retrieveMessage().queue(a -> {
            if (a.getAttachments().isEmpty()) return;
            a.retrieveReactionUsers(EMOJI).queue(b -> {
                b.stream().filter(user -> user.getId().equals(dEvent.getJDA().getSelfUser().getId())).findFirst().ifPresent(c -> {
                    a.clearReactions(EMOJI).queue();
                    createGists(a, dEvent.getUser());
                });
            });
        });

    }
}
