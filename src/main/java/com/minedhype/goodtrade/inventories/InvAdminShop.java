package com.minedhype.goodtrade.inventories;

import com.minedhype.goodtrade.*;
import com.minedhype.goodtrade.gui.GUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.Objects;
import java.util.Optional;

public class InvAdminShop extends GUI {
    public static boolean remoteManage = GoodTrade.config.getBoolean("remoteManage");
    public static boolean stockGUIShop = GoodTrade.config.getBoolean("enableStockAccessFromShopGUI");
    public static boolean stockCommandEnabled = GoodTrade.config.getBoolean("enableStockCommand");
    public static boolean usePerms = GoodTrade.config.getBoolean("usePermissions");
    public static int maxPages = GoodTrade.config.getInt("stockPages");
    public static int permissionMax = GoodTrade.config.getInt("maxStockPages");
    private final Shop shop;

    public InvAdminShop(Shop shop, Player player) {
        super(54, getShopName(shop));
        this.shop = shop;
        updateItems(player);
    }

    private static String getShopName(Shop shop) {
        String shopId = String.valueOf(shop.shopId());
        if (shop.isAdmin())
            return Messages.SHOP_TITLE_ADMIN_SHOP.toString().replaceAll("%id", shopId);
        String msg = Messages.SHOP_TITLE_NORMAL_SHOP.toString().replaceAll("%id", shopId);
        OfflinePlayer pl = Bukkit.getOfflinePlayer(shop.getOwner());
        if (pl == null)
            return msg.replaceAll("%player%", "<unknown>");
        return msg.replaceAll("%player%", Objects.requireNonNull(pl.getName())).replaceAll("%id", shopId);
    }

    private void updateItems(Player player) {
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 6; y++) {
                if (x == 1) {
                    if (y == 0)
                        placeItem(9 + x, GUI.createItem(Material.GREEN_STAINED_GLASS_PANE, ChatColor.GREEN + Messages.SHOP_TITLE_SELL.toString()));
                    else {
                        Optional<RowStore> row = shop.getRow(y - 1);
                        if (row.isPresent())
                            placeItem(y * 9 + x, row.get().getItemOut());
                    }
                } else if (x == 2) {
                    if (y == 0)
                        placeItem(x, GUI.createItem(Material.GREEN_STAINED_GLASS_PANE, ChatColor.GREEN + Messages.SHOP_TITLE_SELL2.toString()));
                    else {
                        Optional<RowStore> row = shop.getRow(y - 1);
                        if (row.isPresent())
                            placeItem(y * 9 + x, row.get().getItemOut2());
                    }
                } else if (x == 4) {
                    if (y == 0)
                        placeItem(x, GUI.createItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + Messages.SHOP_TITLE_BUY.toString()));
                    else {
                        Optional<RowStore> row = shop.getRow(y - 1);
                        if (row.isPresent())
                            placeItem(y * 9 + x, row.get().getItemIn());
                    }
                } else if (x == 5) {
                    if (y == 0)
                        placeItem(9 + x, GUI.createItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + Messages.SHOP_TITLE_BUY2.toString()));
                    else {
                        Optional<RowStore> row = shop.getRow(y - 1);
                        if (row.isPresent())
                            placeItem(y * 9 + x, row.get().getItemIn2());
                    }
                } else if (x == 7 && y > 0) {
                    Optional<RowStore> row = shop.getRow(y - 1);
                    final int index = y - 1;
                    if (row.isPresent()) {
                        placeItem(y * 9 + x, GUI.createItem(Material.TNT, ChatColor.BOLD + Messages.SHOP_TITLE_DELETE.toString()), p -> {
                            shop.delete(index);
                            InvAdminShop inv = new InvAdminShop(shop, p.getPlayer());
                            inv.open(p);
                        });
                    } else {
                        placeItem(y * 9 + x, GUI.createItem(Material.LIME_DYE, ChatColor.BOLD + Messages.SHOP_TITLE_CREATE.toString()), p -> {
                            InvCreateRow inv = new InvCreateRow(shop, index);
                            inv.open(p);
                        });
                    }
                } else if (x == 7 && y == 0) {
                    if (stockGUIShop && !shop.isAdmin() && shop.getOwner().equals(player.getUniqueId())) {
                        placeItem(x, GUI.createItem(Material.CHEST, Messages.SHOP_TITLE_STOCK.toString()), p -> {
                            p.closeInventory();
                            InvStock inv = InvStock.getInvStock(player.getUniqueId());
                            int maxStockPages = maxPages;
                            if (usePerms) {
                                String permPrefix = Permission.SHOP_STOCK_PREFIX.toString();
                                for (PermissionAttachmentInfo attInfo : player.getEffectivePermissions()) {
                                    String perm = attInfo.getPermission();
                                    if (perm.startsWith(permPrefix)) {
                                        int num;
                                        try {
                                            num = Integer.parseInt(perm.substring(perm.lastIndexOf(".") + 1));
                                        } catch (Exception e) {
                                            num = maxPages;
                                        }
                                        if (num > permissionMax)
                                            maxStockPages = permissionMax;
                                        else if (num > 0)
                                            maxStockPages = num;
                                        else
                                            maxStockPages = maxPages;
                                    }
                                }
                            }
                            inv.setMaxPages(maxStockPages);
                            inv.setPag(0);
                            InvStock.inShopInv.put(player, player.getUniqueId());
                            inv.open(player);
                        });
                    } else
                        placeItem(9 + x, GUI.createItem(Material.BLACK_STAINED_GLASS_PANE, ""));
                } else if (x == 8 && y == 0) {
                    if (InvShop.listAllShops && Shop.showOwnedShops && remoteManage) {
                        placeItem(9 + x, GUI.createItem(Material.END_CRYSTAL, Messages.SHOP_LIST_ALL.toString()), p -> {
                            p.closeInventory();
                            p.performCommand("shop shops");
                        });
                    } else
                        placeItem(9 + x, GUI.createItem(Material.BLACK_STAINED_GLASS_PANE, ""));
                } else if (x == 8 && y >= 1) {
                    final Optional<RowStore> row = shop.getRow(y - 1);
                    if (shop.isAdmin()) {
                        if (row.isPresent()) {
                            if (row.get().broadcast) {
                                placeItem(y * 9 + x, GUI.createItem(Material.REDSTONE_TORCH, Messages.SHOP_TITLE_BROADCAST_ON.toString(), (short) 15), p -> {
                                    row.get().toggleBroadcast();
                                    updateItems(player);
                                });
                            } else {
                                placeItem(y * 9 + x, GUI.createItem(Material.LEVER, Messages.SHOP_TITLE_BROADCAST_OFF.toString(), (short) 15), p -> {
                                    row.get().toggleBroadcast();
                                    updateItems(player);
                                });
                            }
                        } else
                            placeItem(y * 9 + x, GUI.createItem(Material.BLACK_STAINED_GLASS_PANE, ""));
                    } else {
                        if (row.isPresent()) {
                            if (row.get().getItemOut().isSimilar(row.get().getItemOut2()) && !Utils.hasDoubleItemStock(shop, row.get().getItemOut(), row.get().getItemOut2()))
                                placeItem(y * 9 + x, GUI.createItem(Material.RED_DYE, Messages.SHOP_NO_STOCK_BUTTON.toString()));
                            else if (!Utils.hasStock(shop, row.get().getItemOut()))
                                placeItem(y * 9 + x, GUI.createItem(Material.RED_DYE, Messages.SHOP_NO_STOCK_BUTTON.toString()));
                            else if (!Utils.hasStock(shop, row.get().getItemOut2()))
                                placeItem(y * 9 + x, GUI.createItem(Material.RED_DYE, Messages.SHOP_NO_STOCK_BUTTON.toString()));
                            else
                                placeItem(y * 9 + x, GUI.createItem(Material.BLACK_STAINED_GLASS_PANE, ""));
                        } else
                            placeItem(y * 9 + x, GUI.createItem(Material.BLACK_STAINED_GLASS_PANE, ""));
                    }
                } else
                    placeItem(y * 9 + x, GUI.createItem(Material.BLACK_STAINED_GLASS_PANE, ""));
            }
        }
    }
}
