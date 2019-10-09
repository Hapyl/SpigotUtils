import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class ScoreboardBuilder {

    private Objective obj;
    private Scoreboard score;
    private int scoreLines;

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
    public ScoreboardBuilder(String scoreName, String displayName, int lines) {

        if (lines > ChatColor.ALL_CODES.length()) {
            throw new IndexOutOfBoundsException("Lines is out of bounds. Max lines is " + ChatColor.ALL_CODES.length());
        }

        score = Bukkit.getScoreboardManager().getNewScoreboard();
        obj = score.registerNewObjective(scoreName, "dummy", "def");
        obj.setDisplayName(f(displayName));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (int i = 1; i < (lines + 1); ++i) {
            score.registerNewTeam("line" + i).addEntry("§" + ChatColor.ALL_CODES.charAt(i));
            obj.getScore("§" + ChatColor.ALL_CODES.charAt(i)).setScore(i);
        }

        scoreLines = lines;
    }

    /**
     * Shows scoreboard to the player.
     * @param player who to show.
     */
    public void show(Player player) {
        player.setScoreboard(score);
    }

    /**
     * Returns current value of the line.
     * @param line line number.
     * @return String.
     */
    public String get(int line) {
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
    public void set(int line, String text) {

        if (line > scoreLines) {
            throw new IndexOutOfBoundsException("Scoreboard has only " + scoreLines + " lines. Given " + line + " line.");
        }

        score.getTeam("line" + line).setSuffix(f(text));
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
    public void updateTitle(String newTitle) {
        obj.setDisplayName(newTitle);
    }

    /**
     * Just an util to translate char color. Ignore it.
     */
    private static String f(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
