package org.mangorage.mangobotgithub.core;

import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
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


public final class GHPRStatus extends TimerTask {

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

	public GHPRStatus(JDAPlugin JDAPlugin) {
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
				String[] repos = config.GIT_REPOS_PR_SCANNED.get().contains(",") ? config.GIT_REPOS_PR_SCANNED.get().split(",") : new String[] {config.GIT_REPOS_PR_SCANNED.get()};
				if (repos.length == 0) continue;
				int prs = 0;
				StringBuilder builder = new StringBuilder();

				for (String repo: repos) {
					int lastChecked = get(getFile(repo));
					int number = lastChecked;
                    GHRepository repository = github.getRepository(repo);
                    List<GHPullRequest> PRS = new ArrayList<GHPullRequest>(repository.getPullRequests(GHIssueState.OPEN).stream().filter(pr -> pr.getNumber() > lastChecked).toList());

					if (!PRS.isEmpty()) {
						prs = prs + PRS.size();
						builder.append("New ").append(repo).append(" PR's: %s".formatted(PRS.size())).append("\n");

						for (GHPullRequest PR: PRS) {
							System.out.println(PR.getNumber());

							if (PR.getNumber() > number)
								number = PR.getNumber();
							builder.append(
								"- %s [%s](%s)"
								.formatted(
									PR.getTitle(),
									PR.getNumber(),
									PR.getHtmlUrl()
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
		return pl.getPluginDirectory().resolve("ghprstatus/" + repo.replace("/", ".") + ".txt");
	}

}