package com.example.christmaspresents;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class DropRateGUI implements Listener {
    private final ChristmasPresents plugin;
    private final Component MAIN_TITLE = Component.text("Present Drop Rates").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
    private final String LIST_TITLE_PREFIX = "Items: ";
    private final NamespacedKey entryKey;
    private final Map<UUID, String> openType = new HashMap<>();
    private final Map<UUID, Integer> openPage = new HashMap<>();
    private static final int ITEMS_PER_PAGE = 45;

    public DropRateGUI(ChristmasPresents plugin) {
        this.plugin = plugin;
        this.entryKey = new NamespacedKey(plugin, "entry_id");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, MAIN_TITLE);
        
        ItemStack commonPresents = createMenuItem(
            Material.CHEST,
            Component.text("Common Presents").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
            Component.text("Click to configure drop rates").color(NamedTextColor.GRAY).decorate(TextDecoration.BOLD),
            Component.text("for common presents.").color(NamedTextColor.GRAY)
        );
        
        ItemStack specialPresents = createMenuItem(
            Material.ENDER_CHEST,
            Component.text("Special Presents").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
            Component.text("Click to configure drop rates").color(NamedTextColor.GRAY).decorate(TextDecoration.BOLD),
            Component.text("for special presents.").color(NamedTextColor.GRAY)
        );
            
        inv.setItem(3, commonPresents);
        inv.setItem(5, specialPresents);
        ItemStack filler = createMenuItem(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, filler);
        
        player.openInventory(inv);
        openType.remove(player.getUniqueId());
        openPage.remove(player.getUniqueId());
    }
    
    public void openItemList(Player player, String presentType) {
        openItemList(player, presentType, 0);
    }
    
    public void openItemList(Player player, String presentType, int page) {
        List<ChristmasPresents.PresentEntry> list = plugin.getDrops().getOrDefault(presentType, java.util.Collections.emptyList());
        int totalPages = Math.max(1, (int) Math.ceil(list.size() / (double) ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, list.size());
        
        Component title = Component.text(LIST_TITLE_PREFIX).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
            .append(Component.text(presentType + " (" + (page + 1) + "/" + totalPages + ")").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        Inventory inv = Bukkit.createInventory(null, 54, title);
        
        for (int i = startIndex; i < endIndex; i++) {
            ChristmasPresents.PresentEntry entry = list.get(i);
            ItemStack item;
            if (entry.kind == ChristmasPresents.EntryKind.EFFECT) {
                Material mat = Material.PAPER;
                String name = "Effect";
                if ("snow_overlay".equalsIgnoreCase(entry.effectKey)) { mat = Material.SNOWBALL; name = "Snow Overlay"; }
                else if ("random_message".equalsIgnoreCase(entry.effectKey)) { mat = Material.WRITABLE_BOOK; name = "Random Message"; }
                else if ("money_reward".equalsIgnoreCase(entry.effectKey)) { mat = Material.GOLD_INGOT; name = "Money Reward"; }
                item = new ItemStack(mat);
                ItemMeta m = item.getItemMeta();
                if (m != null) {
                    m.displayName(Component.text(name).color(NamedTextColor.AQUA));
                    item.setItemMeta(m);
                }
            } else {
                item = entry.item.clone();
            }
            double chance = entry.chance;
            
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.text(" "));
                lore.add(Component.text("Drop Chance: ").color(NamedTextColor.GRAY).decorate(TextDecoration.BOLD).append(Component.text(String.format("%.1f%%", chance)).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)));
                if (entry.kind == ChristmasPresents.EntryKind.EFFECT) lore.add(Component.text("Type: ").color(NamedTextColor.GRAY).append(Component.text(entry.effectKey).color(NamedTextColor.AQUA)));
                lore.add(Component.text("Status: ").color(NamedTextColor.GRAY).decorate(TextDecoration.BOLD).append(Component.text(entry.enabled ? "Enabled" : "Disabled").color(entry.enabled ? NamedTextColor.GREEN : NamedTextColor.RED).decorate(TextDecoration.BOLD)));
                lore.add(Component.text(" "));
                lore.add(Component.text("Left-Click ").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD).append(Component.text("+5%").color(NamedTextColor.GRAY)));
                lore.add(Component.text("Right-Click ").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD).append(Component.text("-5%").color(NamedTextColor.GRAY)));
                lore.add(Component.text("Middle-Click ").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD).append(Component.text("toggle").color(NamedTextColor.GRAY)));
                
                meta.lore(lore);
                meta.getPersistentDataContainer().set(entryKey, PersistentDataType.STRING, entry.id);
                item.setItemMeta(meta);
            }
            
            inv.setItem(i - startIndex, item);
        }
        
        ItemStack filler = createMenuItem(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "));
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);
        
        if (page > 0) {
            inv.setItem(45, createMenuItem(Material.ARROW, Component.text("Previous Page").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)));
        }
        if (page < totalPages - 1) {
            inv.setItem(53, createMenuItem(Material.ARROW, Component.text("Next Page").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)));
        }
        inv.setItem(49, createMenuItem(Material.BARRIER, Component.text("Back").color(NamedTextColor.RED).decorate(TextDecoration.BOLD)));
        inv.setItem(50, createMenuItem(Material.EMERALD_BLOCK, Component.text("Save").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)));
        
        player.openInventory(inv);
        openType.put(player.getUniqueId(), presentType);
        openPage.put(player.getUniqueId(), page);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Component title = event.getView().title();
        
        if (title.equals(MAIN_TITLE)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            
            switch (event.getRawSlot()) {
                case 3:
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    openItemList(player, "common");
                    break;
                case 5:
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    openItemList(player, "special");
                    break;
            }
            return;
        }
        
        if (openType.containsKey(player.getUniqueId())) {
            if (event.getClickedInventory() != event.getView().getTopInventory()) return;
            event.setCancelled(true);
            
            String presentType = openType.get(player.getUniqueId());
            int currentPage = openPage.getOrDefault(player.getUniqueId(), 0);
            if (presentType == null) return;
            
            int slot = event.getRawSlot();
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            if (slot == 45 && clicked.getType() == Material.ARROW) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                openItemList(player, presentType, currentPage - 1);
                return;
            }
            if (slot == 53 && clicked.getType() == Material.ARROW) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                openItemList(player, presentType, currentPage + 1);
                return;
            }
            if (slot == 49 && clicked.getType() == Material.BARRIER) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.9f);
                openMainMenu(player);
                return;
            }
            if (slot == 50 && clicked.getType() == Material.EMERALD_BLOCK) {
                plugin.saveDropsFromMemory();
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);
                player.sendMessage(Component.text("Saved drop rates").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
                return;
            }
            
            if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;
            
            if (slot >= 0 && slot < 45) {
                ItemMeta meta = clicked.getItemMeta();
                if (meta == null) return;
                String id = meta.getPersistentDataContainer().get(entryKey, PersistentDataType.STRING);
                if (id == null) return;
                
                ChristmasPresents.PresentEntry entry = findEntryById(presentType, id);
                if (entry == null) return;
                
                ClickType click = event.getClick();
                if (click == ClickType.LEFT) {
                    entry.chance = Math.min(100.0, entry.chance + 5.0);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.1f);
                    openItemList(player, presentType, currentPage);
                } else if (click == ClickType.RIGHT) {
                    entry.chance = Math.max(0.0, entry.chance - 5.0);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.9f);
                    openItemList(player, presentType, currentPage);
                } else if (click == ClickType.MIDDLE || click == ClickType.CREATIVE) {
                    entry.enabled = !entry.enabled;
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, entry.enabled ? 1.4f : 0.6f);
                    player.sendMessage(Component.text(entry.enabled ? "Enabled" : "Disabled").color(entry.enabled ? NamedTextColor.GREEN : NamedTextColor.RED).decorate(TextDecoration.BOLD));
                    openItemList(player, presentType, currentPage);
                }
            }
        }
    }
    
    private ChristmasPresents.PresentEntry findEntryById(String presentType, String id) {
        if (id == null) return null;
        List<ChristmasPresents.PresentEntry> list = plugin.getDrops().getOrDefault(presentType, java.util.Collections.emptyList());
        for (ChristmasPresents.PresentEntry e : list) if (e.id.equals(id)) return e;
        return null;
    }
    
    private ItemStack createMenuItem(Material material, Component name, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            if (lore != null && lore.length > 0) meta.lore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
