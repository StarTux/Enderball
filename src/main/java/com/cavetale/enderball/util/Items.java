package com.cavetale.enderball.util;

import com.cavetale.enderball.GameTeam;
import java.util.Base64;
import java.util.List;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class Items {
    private Items() { }

    public static String serialize(ItemStack item) {
        byte[] bytes = item.serializeAsBytes();
        String result = Base64.getEncoder().encodeToString(bytes);
        return result;
    }

    public static ItemStack deserialize(String serialized, String name, GameTeam team) {
        byte[] bytes = Base64.getDecoder().decode(serialized);
        ItemStack item = ItemStack.deserializeBytes(bytes);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.values());
        meta.setDisplayName(team.chatColor + name);
        item.setItemMeta(meta);
        item.setAmount(1);
        return item;
    }

    public static void give(Player player, ItemStack... items) {
        for (ItemStack drop : player.getInventory().addItem(items).values()) {
            player.getWorld().dropItem(player.getEyeLocation(), drop);
        }
    }

    public static void give(Player player, List<ItemStack> list) {
        ItemStack[] array = list.toArray(new ItemStack[0]);
        give(player, array);
    }

    public static DyeColor getDyeColor(Material material) {
        String materialName = material.name();
        for (DyeColor dyeColor : DyeColor.values()) {
            if (materialName.startsWith(dyeColor.name())) return dyeColor;
        }
        return null;
    }
}
