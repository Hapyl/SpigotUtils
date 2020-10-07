your package goes here

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.function.Consumer;

public final class MenuBuilder implements Listener {

    public static final Set<MenuBuilder> globalStorage = new HashSet<>();
    public static final Map<Player, MenuBuilder> lastMenu = new HashMap<>();

    private String displayName;
    private int slots, closeMenuSlot = 999;
    private Inventory inventory;
    private Map<Integer, Set<ClickAction>> eventPerSlot = new HashMap<>();
    private Consumer<Player> atClose, outside;
    private boolean cancelClick = true, built, wipe = false;
    private Sound openSound, closeSound;
    private UUID owner;
    private float openSoundPitch, closeSoundPitch;

    private static boolean registered = false;

    private MenuBuilder() {
        // use the MenuBuilder(Plugin main) method to register your events!
        // use the MenuBuilder(Plugin main) method to register your events!
        // use the MenuBuilder(Plugin main) method to register your events!
    }

    // use this to register an event
    public MenuBuilder(Plugin main) {
        if (registered) {
            throw new IllegalArgumentException("Class is already registered!");
        }
        main.getServer().getPluginManager().registerEvents(this, main);
        registered = true;
    }

    public MenuBuilder(String displayName, int slots) {
        this(null, displayName, slots);
    }

    public MenuBuilder(Player owner, String displayName, int slots) {
        if (slots % 9 != 0) {
            Bukkit.getLogger().warning("Slots must be dividable by 9!");
            return;
        }
        if (owner != null) {
            this.owner = owner.getUniqueId();
            this.wipe = true;
        }
        this.slots = slots;
        this.displayName = format(displayName);
        this.inventory = Bukkit.createInventory(null, this.slots, this.displayName);
    }

    private static final ItemStack CLOSE_MENU = new ItemBuilder(Material.BARRIER).setName("&aClose Menu").build();

    public MenuBuilder setCloseMenuItem(int slot) {
        this.closeMenuSlot = slot;
        setItem(slot, CLOSE_MENU);
        return this;
    }

    public MenuBuilder addOpenSound(Sound sound, float pitch) {
        this.openSound = sound;
        this.openSoundPitch = pitch;
        return this;
    }

    public MenuBuilder addCloseSound(Sound sound, float pitch) {
        this.closeSound = sound;
        this.closeSoundPitch = pitch;
        return this;
    }

    public MenuBuilder setItem(int slot, ItemStack item) {
        return setItem(slot, item, (ClickAction) null);
    }

    public MenuBuilder setItem(int slot, ItemStack item, Consumer<Player> t, ClickType... tt) {
        setItem(slot, item, new ClickAction(t, tt));
        return this;
    }

    public MenuBuilder setItem(int slot, ItemStack item, ClickAction action) {
        if (this.slots < slot) {
            throw new IndexOutOfBoundsException("There is only " + this.slots + " slots! Given " + slot);
        }
        if (action != null) {
            this.addClickEvent(slot, action);
        }
        this.inventory.setItem(slot, item);
        return this;
    }

    public int getSize() {
        return slots;
    }

    public MenuBuilder removeItem(int slot) {
        this.inventory.setItem(slot, null);
        return this;
    }

    public MenuBuilder cancelClick(boolean t) {
        this.cancelClick = t;
        return this;
    }

    public MenuBuilder fill(ItemStack item) {
        for (int i = 0; i < this.inventory.getSize(); i++) {
            setItem(i, item);
        }
        return this;
    }

    public MenuBuilder removeClickEvent(int slot) {
        eventPerSlot.remove(slot);
        return this;
    }

    public MenuBuilder addClickEvent(int slot, ClickAction event) {
        final Set<ClickAction> hash = eventPerSlot.getOrDefault(slot, new HashSet<>());
        hash.add(event);
        eventPerSlot.put(slot, hash);
//        hash.clear();
        return this;
    }

    public MenuBuilder addClickEvent(int slot, Consumer<Player> event, ClickType... click) {
        addClickEvent(slot, new ClickAction(event, click));
        return this;
    }

    public MenuBuilder addClickOutsideMenuEvent(Consumer<Player> event) {
        this.outside = event;
        return this;
    }

    public MenuBuilder addCloseEvent(Consumer<Player> event) {
        this.atClose = event;
        return this;
    }

    public void openInventory(Player player) {
        final MenuBuilder t = lastMenu.get(player);
        if (t != null) {
            t.delete();
            lastMenu.remove(player);
        }
        player.openInventory(this.inventory);
        if (this.owner != null && this.owner.equals(player.getUniqueId())) MenuBuilder.lastMenu.put(player, this);
    }

    public MenuBuilder build() {
        this.built = true;
        globalStorage.add(this);
        return this;
    }

    @EventHandler
    public byte handleInventoryOpen(InventoryOpenEvent ev) {

        final InventoryView view = ev.getView();
        final Player player = (Player) ev.getPlayer();

        if (globalStorage.isEmpty()) return -1;
        for (MenuBuilder builder : globalStorage) {
            if (view.getType() == InventoryType.CHEST && view.getTitle().equals(builder.displayName) && builder.inventory != null) {

                // don't do anything if the menu doesn't belong to the player.
                if (builder.owner != null && !builder.owner.equals(player.getUniqueId())) {
                    return -2;
                }

                if (builder.openSound != null) {
                    player.playSound(player.getLocation(), builder.openSound, SoundCategory.MASTER, 2, builder.openSoundPitch);
                    return 1;
                }
            }
        }
        return 0;

    }

    @EventHandler
    private byte handleInventoryClick(InventoryClickEvent ev) {

        final InventoryView view = ev.getView();
        final Player player = (Player) ev.getWhoClicked();
        final ClickType action = ev.getClick();
        final int raw = ev.getRawSlot();

        if (globalStorage.isEmpty()) return -1;
        for (MenuBuilder builder : globalStorage) {
            if (view.getType() == InventoryType.CHEST && view.getTitle().equals(builder.displayName) && builder.inventory != null) {
                if (builder.cancelClick) ev.setCancelled(true);

                if (!builder.built) {
                    throw new MenuBuilderException("Menu you trying access hasn't built yet!");
                }

                // don't do anything if the menu doesn't belong to the player.
                if (builder.owner != null && !builder.owner.equals(player.getUniqueId())) {
                    return -2;
                }

                if (builder.closeMenuSlot == raw) {
                    player.closeInventory();
                    return 4;
                }

                if (builder.outside != null && raw == -999) {
                    builder.outside.accept(player);
                }

                final Map<Integer, Set<ClickAction>> hash = builder.eventPerSlot;
                if (hash.isEmpty()) return -2;

                hash.forEach((slot, act) -> {
                    if (slot == raw) {
                        for (ClickAction g : act) {
                            if (g.actions.contains(action)) {
                                g.consumer.accept(player);
                            }
                        }
                    }
                });
                return 1;
            }
        }
        return 0;
    }

    @EventHandler
    private byte handleEventClose(InventoryCloseEvent ev) {

        final InventoryView view = ev.getView();
        final Player player = (Player) ev.getPlayer();

        if (globalStorage.isEmpty()) return -1;
        final Set<MenuBuilder> hash = new HashSet<>(globalStorage);

        for (MenuBuilder builder : hash) {
            if (view.getType() == InventoryType.CHEST && view.getTitle().equals(builder.displayName) && builder.inventory != null) {

                // don't do anything if the menu doesn't belong to the player.
                if (builder.owner != null && !builder.owner.equals(player.getUniqueId())) {
                    return -2;
                }

                if (builder.closeSound != null) {
                    player.playSound(player.getLocation(), builder.closeSound, SoundCategory.MASTER, 2, builder.closeSoundPitch);
                }

                if (builder.atClose != null) {
                    builder.atClose.accept(player);
                }

            }
        }

        hash.clear();

        return 0;

    }

    public void delete() {
        globalStorage.remove(this);
    }

    public static class ClickAction {

        private Consumer<Player> consumer;
        private Set<ClickType> actions = new HashSet<>();

        public ClickAction(Consumer<Player> consumer, ClickType... actions) {
            if (actions.length < 1) this.actions.addAll(Arrays.asList(ClickType.values()));
            else this.actions.addAll(Arrays.asList(actions));
            this.consumer = consumer;
        }

    }

    private static class MenuBuilderException extends RuntimeException {
        MenuBuilderException(String a) {
            super(a);
        }
    }

    private static String format(String arg0, Object... arg1) {
        return String.format(ChatColor.translateAlternateColorCodes('&', arg0), arg1);
    }


}
