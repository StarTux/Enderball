package com.cavetale.enderball.util;

import java.util.Base64;
import java.util.List;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
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

    public static ItemStack deserialize(String serialized) {
        byte[] bytes = Base64.getDecoder().decode(serialized);
        return ItemStack.deserializeBytes(bytes);
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

    public static void glow(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.values());
        itemStack.setItemMeta(meta);
    }
}
