package org.mangorage.mangobotgithub.core;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.mangorage.mangobot.MangoBotPlugin;
import org.mangorage.mangobotapi.core.plugin.extra.JDAPlugin;
import org.mangorage.mangobotgithub.MangoBotGithub;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public final class GHIssueStatus extends TimerTask {

	public static ArrayList<String> indexed_channels = new ArrayList<String>();

	public static void save(int number, Path fileName) {
		try {
			File file = fileName.toFile();

			// Create the file if it doesn't exist
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
				// Write the number to the file
				writer.write(Integer.toString(number));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static int get(Path fileName) {
		int result = 0;
		File file = fileName.toFile();

		// Check if the file exists before reading
		if (file.exists()) {
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				// Read the number from the file
				String line = reader.readLine();

				if (line != null) {
					result = Integer.parseInt(line.trim());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return result;
	}

	private final JDAPlugin pl;

	public GHIssueStatus(JDAPlugin JDAPlugin) {
		this.pl = JDAPlugin;
		new Timer().scheduleAtFixedRate(this, 15 * 1000, 60 * 60 * 1000); // 60 minutes/1hr
	}

	@Override
	public void run() {
		try {
			String token = MangoBotGithub.GITHUB_TOKEN.get();

			GitHub github = GitHub.connect(MangoBotGithub.GITHUB_USERNAME.get(), token);

			for (String chan: indexed_channels) {
				String guild = pl.getJDA().getTextChannelById(chan).getGuild().getId();
				GuildConfig config = GuildConfig.guildsConfig(guild);
				String[] repos = config.GIT_REPOS_ISSUE_SCANNED.get().contains(",") ? config.GIT_REPOS_ISSUE_SCANNED.get().split(",") : new String[]{config.GIT_REPOS_ISSUE_SCANNED.get()};
				if (repos.length == 0) continue;
				int issues = 0;
				StringBuilder builder = new StringBuilder();

				for (String repo: repos) {
					int lastChecked = get(getFile(repo));
					int number = lastChecked;
                    GHRepository repository = github.getRepository(repo);
					repository.getIssues(GHIssueState.OPEN);
					repository.getIssues(GHIssueState.OPEN).stream();
                    List<GHIssue> ISSUES = new ArrayList<GHIssue>(repository.getIssues(GHIssueState.OPEN).stream().filter(pr -> pr.getNumber() > lastChecked).toList());

					if (!ISSUES.isEmpty()) {
						issues = issues + ISSUES.size();
						builder.append("New ").append(repo).append(" Issue's: %s".formatted(ISSUES.size())).append("\n");

						for (GHIssue issue: ISSUES) {
							System.out.println(issue.getNumber());

							if (issue.getNumber() > number)
								number = issue.getNumber();
							builder.append(
								"- %s [%s](%s)"
								.formatted(
									issue.getTitle(),
									issue.getNumber(),
									issue.getHtmlUrl()
								)
							).append("\n");
						}

						save(number, getFile(repo));

					}

				}

				if (!builder.isEmpty()) {
					var channel = pl.getJDA().getTextChannelById(chan);
					if (channel == null) return;
					channel.sendMessage(builder).setSuppressEmbeds(true).queue();
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Path getFile(String repo) {
		return pl.getPluginDirectory().resolve("ghissuestatus/" + repo.replace("/", ".") + ".txt");
	}

}