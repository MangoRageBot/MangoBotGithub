package org.mangorage.mangobotgithub.core;

import net.dv8tion.jda.api.entities.Message;
import org.mangorage.mangobot.core.BotPermissions;
import org.mangorage.mangobotapi.core.commands.Arguments;
import org.mangorage.mangobotapi.core.commands.CommandResult;
import org.mangorage.mangobotapi.core.commands.IBasicCommand;
import org.mangorage.mangobotapi.core.plugin.extra.JDAPlugin;
import org.mangorage.mangobotapi.core.util.MessageSettings;

import java.util.ArrayList;
import java.util.Arrays;


public final class IssueScanCommand implements IBasicCommand {

	private final JDAPlugin plugin;

	public IssueScanCommand(JDAPlugin plugin) {
		this.plugin = plugin;
	}

	@Override

	public CommandResult execute(Message message, Arguments args) {
		// TODO Auto-generated method stub
		MessageSettings dMessage = plugin.getMessageSettings();

		String type = args.get(0);
		String answer = args.get(1);
		GuildConfig guildConfig = GuildConfig.guildsConfig(message.getGuildId());

		if (!BotPermissions.TRICK_ADMIN.hasPermission(message.getMember())) {
			dMessage.apply(message.reply("No permission!")).queue();
			return CommandResult.NO_PERMISSION;
		}

		if (!type.equals("") && !type.equals(" ")) {
			if (type.equals("-add")) {
				String[] repos_arr = guildConfig.GIT_REPOS_ISSUE_SCANNED.get().split(",");
				ArrayList<String> repos = new ArrayList<String> (Arrays.asList(repos_arr));
				repos.add(answer);
				String result = String.join(",", repos);
				guildConfig.GIT_REPOS_ISSUE_SCANNED.set(result);
				dMessage.apply(message.reply("Added: " + answer)).queue();
			} else if (type.equals("-remove")) {
				String[] repos_arr = guildConfig.GIT_REPOS_ISSUE_SCANNED.get().split(",");
				ArrayList<String> repos = new ArrayList<String> (Arrays.asList(repos_arr));
				repos.remove(answer);
				String result = String.join(",", repos);
				guildConfig.GIT_REPOS_ISSUE_SCANNED.set(result);
				dMessage.apply(message.reply("Removed: " + answer)).queue();
			} else if (type.equals("-list")) {
				String[] repos = guildConfig.GIT_REPOS_ISSUE_SCANNED.get().split(",");
				StringBuilder builder = new StringBuilder();

				for (String repo: repos) {
					builder.append(repo + "\n");
				}

				dMessage.apply(message.reply(builder)).queue();
			} else if (type.equals("-setchannel")) {
				GHIssueStatus.indexed_channels.remove(guildConfig.GIT_REPOS_ISSUE_SCANNED_CHANNELID.get());

				if (answer == null) {
					answer = message.getChannelId();
				}

				guildConfig.GIT_REPOS_ISSUE_SCANNED_CHANNELID.set(answer);
				GHIssueStatus.indexed_channels.add(answer);
				dMessage.apply(message.reply("Set Channel")).queue();
			} else {
				dMessage.apply(message.reply("Invalid arg " + type)).queue();
				return CommandResult.FAIL;
			}

		} else {
			dMessage.apply(message.reply("""
                    Issue Scan Command Usage:
                    ``!issuescan -add Org/Repo`` Adds a Repository
                    ``!issuescan -remove Org/Repo`` Removes a Repository
                    ``!issuescan -list`` Lists Indexed Repository
                    ``!issuescan -setchannel`` Sets this as the current channel to list the issues
                    """
			)).queue();
		}

		return CommandResult.PASS;
	}

	@Override

	public String commandId() {
		return "issuescan";
	}

}