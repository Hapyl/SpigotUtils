import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class ScoreboardBuilder {

    private static Objective obj;
    private static Scoreboard score;
    private static int scoreLines;

    /**
     * Constructor for Scoreboard.
     *
     * Examples:
     *  ScoreboardBuilder score = new ScoreboardBuilder("mainScore", "&e&lMAIN SCOREBOARD", 5); @ Creates new scoreboard with 5 lines.
     *  score.set(3, "&aThis text is cool!") @ Sets text on line 3 to "&aThis text is cool!".
     *  Bukkit.getOnlinePlayers().forEach(player -> score.show(player)); @ Shows everyone one the server the scoreboard.
     *  score.hide(Bukkit.getPlayerExact("hapyl")); @ Hides scoreboard for player with name "hapyl";
     *
     * @param scoreName   name that scoreboard have in code.
     * @param displayName name that shows to players.
     * @param lines       how much line there will be. Max lines 16. Starts with 1.
     */
    ScoreboardBuilder(String scoreName, String displayName, int lines) {

        if (lines > ChatColor.ALL_CODES.length()) {
            throw new IndexOutOfBoundsException("Lines is out of bounds. Max lines is " + ChatColor.ALL_CODES.length());
        }

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        obj = scoreboard.registerNewObjective(scoreName, "dummy");
        obj.setDisplayName(f(displayName));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (int i = 1; i < lines; i++) {
            scoreboard.registerNewTeam("line" + i).addEntry("§" + ChatColor.ALL_CODES.charAt(i));
            obj.getScore("§" + ChatColor.ALL_CODES.charAt(i)).setScore(i);
        }

        score = scoreboard;
        scoreLines = lines;
    }

    /**
     * Shows scoreboard to the player.
     * @param player who to show.
     */
    public ScoreboardBuilder show(Player player) {
        player.setScoreboard(score);
        return this;
    }

    /**
     * Returns current value of the line.
     * @param line line number.
     * @return String.
     */
    public static String get(int line) {
        if (score.getTeam("line" + line) != null) {
            return score.getTeam("line" + line).getSuffix();
        }
        return "notFound";
    }

    /**
     * Sets text for certain line.
     * If scoreboard doesn't have that line IndexOutBound will be thrown.
     * @param line line number.
     * @param text text to set. (Supports '&' char as color code.)
     */
    public ScoreboardBuilder set(int line, String text) {

        if (line > scoreLines) {
            throw new IndexOutOfBoundsException("Scoreboard has only " + scoreLines + " lines. Given " + line + " line.");
        }

        score.getTeam("line" + line).setSuffix(f(text));
        return this;
    }

    /**
     * Hides scoreboard for player.
     * @param player who to hide.
     */
    public ScoreboardBuilder hide(Player player) {
        for (String entry : score.getEntries()) {
            score.resetScores(entry);
        }
        return this;
    }

    /**
     * Updates title for the scoreboard.
     * By title it means DisplayName.
     * @param newTitle String.
     */
    public ScoreboardBuilder updateTitle(String newTitle) {
        obj.setDisplayName(newTitle);
        return this;
    }

    /**
     * Just an util to translate char color. Ignore it.
     */
    private static String f(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
