package org.mangorage.mangobotgithub;

import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.mangorage.mangobotapi.core.commands.Arguments;
import org.mangorage.mangobotapi.core.commands.CommandResult;
import org.mangorage.mangobotapi.core.commands.CommandType;
import org.mangorage.mangobotapi.core.commands.IBasicCommand;

import java.io.IOException;

public class AICommand implements IBasicCommand {
    @Override
    public @NotNull CommandResult execute(Message message, Arguments arguments) {
        var prompt = arguments.getFrom(0);
        if (prompt.isBlank()) {

        } else {
            try {
                var response = ChatGPTBot.askChatGPT(prompt);
                if (response != null) {
                    var choices = response.getChoices();
                    if (!choices.isEmpty()) {
                        var msg = choices.getFirst().getMessage();
                        if (msg != null) {
                            message.reply(msg.getContent()).setSuppressEmbeds(true).mentionRepliedUser(false).queue();
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }
        return CommandResult.PASS;
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
