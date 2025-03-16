package org.mangorage.mangobotgithub;

import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.mangorage.mangobotapi.core.commands.Arguments;
import org.mangorage.mangobotapi.core.commands.CommandResult;
import org.mangorage.mangobotapi.core.commands.CommandType;
import org.mangorage.mangobotapi.core.commands.IBasicCommand;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AICommand implements IBasicCommand {
    public @NotNull CommandResult execute(Message message, Arguments arguments) {
        var prompt = arguments.getFrom(0);
        if (prompt.isBlank()) {
            return CommandResult.PASS;
        }

        try {
            var response = ChatGPTBot.askChatGPT(prompt);
            if (response != null) {
                var choices = response.getChoices();
                if (!choices.isEmpty()) {
                    var msg = choices.getFirst().getMessage();
                    if (msg != null) {
                        String content = msg.getContent();
                        if (content.length() > 2000) {
                            List<String> parts = splitMessage(content, 2000);
                            for (String part : parts) {
                                message.reply(part).setSuppressEmbeds(true).mentionRepliedUser(false).queue();
                            }
                        } else {
                            message.reply(content).setSuppressEmbeds(true).mentionRepliedUser(false).queue();
                        }
                    }
                }
            }
        } catch (IOException ignored) {}

        return CommandResult.PASS;
    }

    /**
     * Splits a message into smaller parts without breaking words.
     */
    private List<String> splitMessage(String message, int maxLength) {
        List<String> parts = new ArrayList<>();
        while (message.length() > maxLength) {
            int splitIndex = message.lastIndexOf("\n", maxLength);
            if (splitIndex == -1) {
                splitIndex = message.lastIndexOf(" ", maxLength);
            }
            if (splitIndex == -1) {
                splitIndex = maxLength;
            }
            parts.add(message.substring(0, splitIndex));
            message = message.substring(splitIndex).trim();
        }
        parts.add(message);
        return parts;
    }

    @Override
    public String commandId() {
        return "askAI";
    }

    @Override
    public CommandType commandType() {
        return CommandType.BOTH;
    }
}
