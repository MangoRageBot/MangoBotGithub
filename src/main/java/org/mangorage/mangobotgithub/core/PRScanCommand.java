package org.mangorage.mangobotgithub.core;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import org.mangorage.commonutils.jda.MessageSettings;
import org.mangorage.commonutils.misc.Arguments;
import org.mangorage.mangobotcore.jda.command.api.CommandResult;
import org.mangorage.mangobotcore.jda.command.api.ICommand;
import org.mangorage.mangobotplugin.entrypoint.MangoBot;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public final class PRScanCommand implements ICommand {

	private final MangoBot plugin;

	public PRScanCommand(MangoBot plugin) {
		this.plugin = plugin;
	}

	@Override
	public String id() {
		return "prscan";
	}

	@Override
	public List<String> commands() {
		return List.of("prscan");
	}

	@Override
	public String usage() {
		return "PR Scan Usage: N/A";
	}

	@Override
	public CommandResult execute(Message message, Arguments args) {
		MessageSettings dMessage = plugin.getMessageSettings();

		String type = args.get(0);
		String answer = args.get(1);
		GuildConfig guildConfig = GuildConfig.guildsConfig(message.getGuildId());


		if (!message.getMember().hasPermission(Permission.ADMINISTRATOR)) return CommandResult.NO_PERMISSION;


		if (!type.equals("") && !type.equals(" ")) {
			if (type.equals("-add")) {
				String[] repos_arr = guildConfig.GIT_REPOS_PR_SCANNED.get().split(",");
				ArrayList<String> repos = new ArrayList<String> (Arrays.asList(repos_arr));
				repos.add(answer);
				String result = String.join(",", repos);
				guildConfig.GIT_REPOS_PR_SCANNED.set(result);
				dMessage.apply(message.reply("Added: " + answer)).queue();
			} else if (type.equals("-remove")) {
				String[] repos_arr = guildConfig.GIT_REPOS_PR_SCANNED.get().split(",");
				ArrayList<String> repos = new ArrayList<String> (Arrays.asList(repos_arr));
				repos.remove(answer);
				String result = String.join(",", repos);
				guildConfig.GIT_REPOS_PR_SCANNED.set(result);
				dMessage.apply(message.reply("Removed: " + answer)).queue();
			} else if (type.equals("-list")) {
				String[] repos = guildConfig.GIT_REPOS_PR_SCANNED.get().split(",");
				StringBuilder builder = new StringBuilder();

				for (String repo: repos) {
					builder.append(repo + "\n");
				}

				dMessage.apply(message.reply(builder)).queue();
			} else if (type.equals("-setchannel")) {
				GHPRStatus.indexed_channels.remove(guildConfig.GIT_REPOS_PR_SCANNED_CHANNELID.get());

				if (answer == null) {
					answer = message.getChannelId();
				}

				guildConfig.GIT_REPOS_PR_SCANNED_CHANNELID.set(answer);
				GHPRStatus.indexed_channels.add(answer);
				dMessage.apply(message.reply("Set Channel")).queue();
			} else {
				dMessage.apply(message.reply("Invalid arg " + type)).queue();
				return CommandResult.FAIL;
			}

		} else {
			dMessage.apply(message.reply("""
                    PR Scan Command Usage:
                    ``!prscan -add Org/Repo`` Adds a Repository
                    ``!prscan -remove Org/Repo`` Removes a Repository
                    ``!prscan -list`` Lists Indexed Repository
                    ``!prscan -setchannel`` Sets this as the current channel to list the pull requests
                    """
			)).queue();
		}

		return CommandResult.PASS;
	}

}