package YOUR.PACKAGE.GOES.HERE


import net.md_5.bungee.api.chat.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class ConfirmManager implements Listener {

    private static Map<Player, ConfirmManager> confirmStorage = new HashMap<>();

    public ConfirmManager() {
        // For event registration.
    }

    private Player player;
    private String message;
    private long startedAt;
    private long expireLimitMillis;

    // Format:
    // { message , hover message }
    private String[] confirmMessage = {"&a&l[CONFIRM]", "&7Click to confirm!"};
    private String[] cancelMessage = {"&c&l[CANCEL]", "&7Click to cancel!"};

    private Consumer<? super Player> confirmAction;
    private Consumer<? super Player> cancelAction;

    public final String CONFIRM_COMMAND = "+confirm";
    public final String CANCEL_COMMAND = "+cancel";

     /**
     * [!][!][!][!][!][!][!][!][!][!][!][!][!][!][!][!][!][!]
     * [!] Don't forget to register events for this class [!]
     * [!][!][!][!][!][!][!][!][!][!][!][!][!][!][!][!][!][!]
     *
     * Updates:
     *  v 0.2:
     *      - You can now change confirm and cancel messages by using '.setConfirmMessage' and 'setCancelMessage'
     *      - [!] Builder now require '.ask()' at the end to send a message to player.
     *
     * Constructor for confirmation. (v 0.1)
     *      Usage Examples:
     *
     *          new ConfirmManager(<player>, "&aConfrimation message that supports color cosed!", 0, p -> p.sendMessage("You just accepted it!"), p -> p.sendMessage("You just cancelled it!")).ask();
     *          new ConfirmManager(<player>, "&aClick CANCEL fast it'll expire in 1 second!", 1, p -> p.sendMessage("I said CANCEL!"), p -> p.sendMessage("Good Job!")).ask();
     *          new ConfirmManager(<player>, "&eDo you want to get Creative more?", 0, p -> {p.sendMessage("Enjoy."); player.setGameMode(GameMode.CREATIVE);}, p -> {p.sendMessage("k"); player.setGameMode(GameMode.SURVIVAL);}).ask();
     *
     *      Extra Info:
     *          - Leaving the server cancels confirmation.
     *          - Only one confirmation can be used at the same time. (Might change it future, no point for doing this tbh.)
     *          - You can also use '/{@value CONFIRM_COMMAND}' to confirm or '/{@value CANCEL_COMMAND}' to cancel in any time.
     *              - If you don't like the commands, you can change them, but don't forget you can break something.
     *          - Most of the fields and methods are private and can't be accessed, but if you want, you can always create getters, if
     *              you need to values for some reason.
     *
     * @param player - Player who execution it.
     * @param message - Message of the confirmation.
     * @param expireAfterInSec How long it takes to expires. 0 for no expiration.
     * @param confirmAction - Consumer of <player> for confirm action.
     * @param cancelAction - Consumer of <player> for cancel action.
     *                     
     * Todo:
     *  [X] Custom CONFIRM and CANCEL messages.
     *  Multiple confirmations (id's).
     *
     */

    public ConfirmManager(Player player, String message, int expireAfterInSec, Consumer<? super Player> confirmAction, Consumer<? super Player> cancelAction) {

        if (playerHasConfirmation(player)) {
            final ConfirmManager manager = confirmStorage.get(player);
            if (manager.checkExpiration()) {
                player.sendMessage(ChatColor.RED + "You have confirmation pending. Please confirm or cancel it.");
                manager.askConfirmation();
                return;
            }
        }

        this.player = player;
        this.message = colorize(message);
        this.startedAt = System.currentTimeMillis();
        this.expireLimitMillis = expireAfterInSec * 1000;

        this.confirmAction = confirmAction;
        this.cancelAction = cancelAction;

        confirmStorage.put(player, this);

    }

    /**
     * Used to change confirmation message (Colorized with '&')
     *
     * @param message      - Confirmation message.
     * @param hoverMessage - Hover message. Keep empty for no hover effect.
     */
    public ConfirmManager setConfirmMessage(String message, String hoverMessage) {
        this.confirmMessage = new String[]{message, hoverMessage};
        return this;
    }

    /**
     * Used to change cancel message (Colorized with '&')
     *
     * @param message      - Cancel message.
     * @param hoverMessage - Hover message. Keep empty for no hover effect.
     */
    public ConfirmManager setCancelMessage(String message, String hoverMessage) {
        this.cancelMessage = new String[]{message, hoverMessage};
        return this;
    }

    /**
     * Used to send player message about confirmation.
     */
    public void ask() {
        this.askConfirmation();
    }

    private boolean checkExpiration() {
        if (this.expireLimitMillis != 0 && System.currentTimeMillis() - this.startedAt > expireLimitMillis) {
            this.player.sendMessage(colorize("&cThis confirmation has expired!"));
            confirmStorage.remove(this.player);
            return false;
        }
        return true;
    }

    private void askConfirmation() {

        final ComponentBuilder accept = new ComponentBuilder(colorize(this.confirmMessage[0]))
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + this.CONFIRM_COMMAND));

        if (!this.confirmMessage[1].isEmpty())
            accept.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(colorize(this.confirmMessage[1])).create()));

        final ComponentBuilder cancel = new ComponentBuilder(colorize(this.cancelMessage[0]))
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + this.CANCEL_COMMAND));

        if (!this.cancelMessage[1].isEmpty())
            cancel.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(colorize(this.cancelMessage[1])).create()));

        this.player.sendMessage(this.message);
        this.player.spigot().sendMessage(accept.create());
        this.player.spigot().sendMessage(cancel.create());

    }

    private boolean playerHasConfirmation(Player t) {
        return confirmStorage.containsKey(t);
    }

    private void action(boolean confirmOrCancel) {
        if (confirmStorage.containsKey(this.player)) {
            if (confirmOrCancel) this.confirmAction.accept(this.player);
            else this.cancelAction.accept(this.player);
            confirmStorage.remove(this.player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void event(PlayerCommandPreprocessEvent ev) {

        final Player player = ev.getPlayer();
        final String[] message = ev.getMessage().split(" ");

        final String command = message[0].replace("/", "");

        if (command.equalsIgnoreCase(this.CONFIRM_COMMAND) || command.equalsIgnoreCase(this.CANCEL_COMMAND)) {
            ev.setCancelled(true);
            if (playerHasConfirmation(player)) {
                final ConfirmManager manager = confirmStorage.get(player);
                if (manager.checkExpiration()) manager.action(command.equalsIgnoreCase(this.CONFIRM_COMMAND));
            } else
                player.sendMessage(ChatColor.RED + "Nothing to " + (command.equalsIgnoreCase(this.CONFIRM_COMMAND) ? "confirm" : "cancel") + ".");
        }
    }

    private String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

}
