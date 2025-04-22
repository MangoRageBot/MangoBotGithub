package org.mangorage.mangobotgithub.core;


import org.mangorage.commonutils.config.Config;
import org.mangorage.commonutils.config.ConfigSetting;
import org.mangorage.commonutils.config.ISetting;
import org.mangorage.mangobotplugin.entrypoint.MangoBot;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;


public final class GuildConfig {
	public static HashMap<String, GuildConfig> configs = new HashMap<String, GuildConfig>();

	public final Config guildConfig;
	public final ISetting<String> GIT_REPOS_PR_SCANNED;
	public final ISetting<String> GIT_REPOS_PR_SCANNED_CHANNELID;
	public final ISetting<String> GIT_REPOS_ISSUE_SCANNED;
	public final ISetting<String> GIT_REPOS_ISSUE_SCANNED_CHANNELID;

	private GuildConfig(String guildID) {
		var root = MangoBot.CONFIG.getFile().getParent();

		this.guildConfig = new Config(Path.of("%s/guildConfigs/%s/config.conf".formatted(root, guildID)));

		this.GIT_REPOS_PR_SCANNED = ConfigSetting.create(this.guildConfig, "GIT_REPOS_PR_SCANNED", "");
		this.GIT_REPOS_PR_SCANNED_CHANNELID = ConfigSetting.create(this.guildConfig, "GIT_REPOS_PR_SCANNED_CHANNELID", "empty");
		this.GIT_REPOS_ISSUE_SCANNED = ConfigSetting.create(this.guildConfig, "GIT_REPOS_ISSUE_SCANNED", "");
		this.GIT_REPOS_ISSUE_SCANNED_CHANNELID = ConfigSetting.create(this.guildConfig, "GIT_REPOS_ISSUE_SCANNED_CHANNELID", "empty");

		configs.put(guildID, this);

		if (!GIT_REPOS_PR_SCANNED_CHANNELID.get().equals("empty")) {
			GHPRStatus.indexed_channels.add(GIT_REPOS_PR_SCANNED_CHANNELID.get());
		}

		if (!GIT_REPOS_ISSUE_SCANNED_CHANNELID.get().equals("empty")) {
			GHIssueStatus.indexed_channels.add(GIT_REPOS_ISSUE_SCANNED_CHANNELID.get());
		}

	}

	public static GuildConfig guildsConfig(String guildID) {
		if (configs.containsKey(guildID)) {
			return configs.get(guildID);
		}

		return new GuildConfig(guildID);
	}

	public static void loadServerConfigs() {
		Path root = MangoBot.CONFIG.getFile().getParent();
		File path = new File(root + "/guildConfigs/");

		if (path.exists()) {
			File[] listOfFiles = path.listFiles();

			if (listOfFiles != null) {
				for (File file: listOfFiles) {
					if (file.isDirectory()) {
						guildsConfig(file.getName());
					}
				}
			}
		} else {
			System.out.println("No Guild Configs!");
		}

	}

}