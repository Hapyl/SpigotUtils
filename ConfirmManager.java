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

    private Consumer<? super Player> confirmAction;
    private Consumer<? super Player> cancelAction;

    public final String CONFIRM_COMMAND = "+confirm";
    public final String CANCEL_COMMAND = "+cancel";

    /**
     * [!][!][!][!][!][!][!][!][!][!][!][!][!][!][!][!][!][!]
     * [!] Don't forget to register events for this class [!]
     * [!] [!][!][!][!][!][!][!][!][!][!][!][!][!][!][!][!][!]
     *
     * Constructor for confirmation. (v 0.1)
     *      Usage Examples:
     *
     *          new ConfirmManager(<player>, "&aConfrimation message that supports color cosed!", 0, p -> p.sendMessage("You just accepted it!"), p -> p.sendMessage("You just cancelled it!"));
     *          new ConfirmManager(<player>, "&aClick CANCEL fast it'll expire in 1 second!", 1, p -> p.sendMessage("I said CANCEL!"), p -> p.sendMessage("Good Job!"));
     *          new ConfirmManager(<player>, "&eDo you want to get Creative more?", 0, p -> {p.sendMessage("Enjoy."); player.setGameMode(GameMode.CREATIVE);}, p -> {p.sendMessage("k"); player.setGameMode(GameMode.SURVIVAL);});
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
     *  Custom CONFIRM and CANCEL messages.
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
        askConfirmation();

    }

    private boolean checkExpiration() {
        if (this.expireLimitMillis != 0 && System.currentTimeMillis() - this.startedAt > expireLimitMillis) {
            this.player.sendMessage(String.format(colorize("&cThis confirmation has expired!"), ChatColor.stripColor(this.message)));
            confirmStorage.remove(this.player);
            return false;
        }
        return true;
    }

    private void askConfirmation() {

        final BaseComponent[] accept = new ComponentBuilder(colorize("&a&l[CONFIRM]"))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(colorize("&7Click to confirm!")).create()))
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + this.CONFIRM_COMMAND)).create();

        final BaseComponent[] cancel = new ComponentBuilder(colorize("&c&l[CANCEL]"))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(colorize("&7Click to cancel!")).create()))
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + this.CANCEL_COMMAND)).create();

        this.player.sendMessage(this.message);
        this.player.spigot().sendMessage(accept);
        this.player.spigot().sendMessage(cancel);

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