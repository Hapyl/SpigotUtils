import net.md_5.bungee.api.chat.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MessageBuilder {

    private TextComponent message;

    public MessageBuilder(String message) {
        this.message = new TextComponent(ChatColor.translateAlternateColorCodes('&', message));
    }

    public MessageBuilder setClickEvent(ClickEvent.Action a, String value) {
        this.message.setClickEvent(new ClickEvent(a, value));
        return this;
    }

    public MessageBuilder setHoverEvent(HoverEvent.Action a, String value){
        this.message.setHoverEvent(new HoverEvent(a, new ComponentBuilder(value).create()));
        return this;
    }

    public MessageBuilder send(Player player) {
        player.spigot().sendMessage(this.message);
        return this;
    }
    
}
