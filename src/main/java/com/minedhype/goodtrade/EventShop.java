package com.minedhype.goodtrade;

import com.minedhype.goodtrade.inventories.InvAdminShop;
import com.minedhype.goodtrade.inventories.InvShop;
import com.minedhype.goodtrade.inventories.InvStock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EventShop implements Listener {
    public static boolean adminShopEnabled = GoodTrade.config.getBoolean("enableAdminShop");
    public static boolean stockEnabled = GoodTrade.config.getBoolean("enableStockBlock");
    public static boolean multipleShopBlocks = GoodTrade.config.getBoolean("multipleShopBlocks");
    public static boolean multipleStockBlocks = GoodTrade.config.getBoolean("multipleStockBlocks");
    public static boolean shopEnabled = GoodTrade.config.getBoolean("enableShopBlock");
    public static boolean soldJoinMessage = GoodTrade.config.getBoolean("enableSoldNotificationOnJoin");
    public static boolean soldOnlyOnFirstConnect = GoodTrade.config.getBoolean("onlyNotifySoldOnceUntilClear");
    public static boolean noShopNoStock = GoodTrade.config.getBoolean("mustOwnShopForStock");
    public static boolean placeFrameSign = GoodTrade.config.getBoolean("placeItemFrameSigns");
    public static boolean protectShopFromExplosion = GoodTrade.config.getBoolean("protectShopBlocksFromExplosions");
    public static int soldMessageDelayTime = GoodTrade.config.getInt("soldNotificationsDelayTime");
    public static int stockRangeLimit = GoodTrade.config.getInt("stockRangeLimitFromShop");
    public static String shopBlock = GoodTrade.config.getString("shopBlock");
    public static String stockBlock = GoodTrade.config.getString("stockBlock");
    public static Material shopBlk = Material.matchMaterial(shopBlock);
    public static Material stockBlk = Material.matchMaterial(stockBlock);
    public static List<String> multipleShopBlock = GoodTrade.config.getStringList("shopBlockList");
    public static List<String> multipleStockBlock = GoodTrade.config.getStringList("stockBlockList");
    public static final ConcurrentHashMap<UUID, Integer> soldListSent = new ConcurrentHashMap<>();

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!shopEnabled && !stockEnabled)
            return;
        Block block = event.getClickedBlock();
        if (block == null)
            return;
        if (shopBlk == null) {
            try {
                shopBlk = Material.matchMaterial(shopBlock.split("minecraft:")[1].toUpperCase());
            } catch (Exception e) {
                shopBlk = Material.BARREL;
            }
        }
        if (stockBlk == null) {
            try {
                stockBlk = Material.matchMaterial(stockBlock.split("minecraft:")[1].toUpperCase());
            } catch (Exception e) {
                stockBlk = Material.COMPOSTER;
            }
        }
        if (!block.getType().equals(stockBlk) && !block.getType().equals(shopBlk) && !multipleShopBlocks && !multipleStockBlocks)
            return;
        if (block.getType().equals(stockBlk) && block.getType().equals(shopBlk) && shopEnabled) {
            boolean isShopLoc = true;
            Optional<Shop> shop = Shop.getShopByLocation(block.getLocation());
            if (!shop.isPresent() || !isShopLoc)
                return;
            if (placeFrameSign)
                if (event.getPlayer().isSneaking() && event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && (shop.get().isOwner(event.getPlayer().getUniqueId()) || event.getPlayer().hasPermission(Permission.SHOP_ADMIN.toString())) && (event.getPlayer().getInventory().getItemInMainHand().getType().toString().endsWith("_SIGN") || event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.ITEM_FRAME)))
                    return;
            if (shop.get().isAdmin() && !adminShopEnabled) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Messages.ADMIN_SHOP_DISABLED.toString());
                return;
            }
            event.setCancelled(true);
            if (InvStock.inShopInv.containsValue(shop.get().getOwner())) {
                if (event.getHand().equals(EquipmentSlot.HAND))
                    event.getPlayer().sendMessage(Messages.SHOP_BUSY.toString());
                else if (event.getHand().equals(EquipmentSlot.OFF_HAND))
                    event.getPlayer().sendMessage(Messages.SHOP_BUSY.toString());
                return;
            }
            if ((shop.get().isAdmin() && event.getPlayer().hasPermission(Permission.SHOP_ADMIN.toString())) || shop.get().isOwner(event.getPlayer().getUniqueId())) {
                InvAdminShop inv = new InvAdminShop(shop.get(), event.getPlayer());
                inv.open(event.getPlayer(), shop.get().getOwner());
            } else {
                InvShop inv = new InvShop(shop.get());
                inv.open(event.getPlayer(), shop.get().getOwner());
            }
            return;
        }
        if (block.getType().equals(shopBlk) && shopEnabled) {
            boolean isShopLoc = true;
            Optional<Shop> shop = Shop.getShopByLocation(block.getLocation());
            if (!shop.isPresent() || !isShopLoc)
                return;
            if (placeFrameSign)
                if (event.getPlayer().isSneaking() && event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && (shop.get().isOwner(event.getPlayer().getUniqueId()) || event.getPlayer().hasPermission(Permission.SHOP_ADMIN.toString())) && (event.getPlayer().getInventory().getItemInMainHand().getType().toString().endsWith("_SIGN") || event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.ITEM_FRAME)))
                    return;
            if (shop.get().isAdmin() && !adminShopEnabled) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Messages.ADMIN_SHOP_DISABLED.toString());
                return;
            }
            event.setCancelled(true);
            if (InvStock.inShopInv.containsValue(shop.get().getOwner())) {
                if (event.getHand().equals(EquipmentSlot.HAND))
                    event.getPlayer().sendMessage(Messages.SHOP_BUSY.toString());
                else if (event.getHand().equals(EquipmentSlot.OFF_HAND))
                    event.getPlayer().sendMessage(Messages.SHOP_BUSY.toString());
                return;
            }
            if ((shop.get().isAdmin() && event.getPlayer().hasPermission(Permission.SHOP_ADMIN.toString())) || shop.get().isOwner(event.getPlayer().getUniqueId())) {
                InvAdminShop inv = new InvAdminShop(shop.get(), event.getPlayer());
                inv.open(event.getPlayer(), shop.get().getOwner());
            } else {
                InvShop inv = new InvShop(shop.get());
                inv.open(event.getPlayer(), shop.get().getOwner());
            }
            return;
        }
        if (multipleShopBlocks && shopEnabled) {
            for (String shopBlocks : multipleShopBlock) {
                Material shopListBlocks = Material.matchMaterial(shopBlocks);
                if (shopListBlocks != null && block.getType().equals(shopListBlocks)) {
                    boolean isShopLoc = true;
                    Optional<Shop> shop = Shop.getShopByLocation(block.getLocation());
                    if (!shop.isPresent() || !isShopLoc)
                        return;
                    if (placeFrameSign)
                        if (event.getPlayer().isSneaking() && event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && (shop.get().isOwner(event.getPlayer().getUniqueId()) || event.getPlayer().hasPermission(Permission.SHOP_ADMIN.toString())) && (event.getPlayer().getInventory().getItemInMainHand().getType().toString().endsWith("_SIGN") || event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.ITEM_FRAME)))
                            return;
                    if (shop.get().isAdmin() && !adminShopEnabled) {
                        event.setCancelled(true);
                        event.getPlayer().sendMessage(Messages.ADMIN_SHOP_DISABLED.toString());
                        return;
                    }
                    event.setCancelled(true);
                    if (InvStock.inShopInv.containsValue(shop.get().getOwner())) {
                        if (event.getHand().equals(EquipmentSlot.HAND))
                            event.getPlayer().sendMessage(Messages.SHOP_BUSY.toString());
                        else if (event.getHand().equals(EquipmentSlot.OFF_HAND))
                            event.getPlayer().sendMessage(Messages.SHOP_BUSY.toString());
                        return;
                    }
                    if ((shop.get().isAdmin() && event.getPlayer().hasPermission(Permission.SHOP_ADMIN.toString())) || shop.get().isOwner(event.getPlayer().getUniqueId())) {
                        InvAdminShop inv = new InvAdminShop(shop.get(), event.getPlayer());
                        inv.open(event.getPlayer(), shop.get().getOwner());
                    } else {
                        InvShop inv = new InvShop(shop.get());
                        inv.open(event.getPlayer(), shop.get().getOwner());
                    }
                    return;
                }
            }
        }
        if (block.getType().equals(stockBlk) && stockEnabled) {
            boolean isShopLoc = true;
            if (!isShopLoc || event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getPlayer().isSneaking())
                return;
            event.setCancelled(true);
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                if (Shop.getNumShops(event.getPlayer().getUniqueId()) < 1 && noShopNoStock) {
                    event.getPlayer().sendMessage(Messages.NO_SHOP_STOCK.toString());
                    return;
                }
                if (stockRangeLimit > 0)
                    if (!Shop.checkShopDistanceFromStockBlock(event.getClickedBlock().getLocation(), event.getPlayer().getUniqueId())) {
                        event.getPlayer().sendMessage(Messages.SHOP_FAR.toString());
                        return;
                    }
                if (InvStock.inShopInv.containsValue(event.getPlayer().getUniqueId())) {
                    event.getPlayer().sendMessage(Messages.SHOP_BUSY.toString());
                    return;
                } else {
                    InvStock.inShopInv.put(event.getPlayer(), event.getPlayer().getUniqueId());
                }
                InvStock inv = InvStock.getInvStock(event.getPlayer().getUniqueId());
                int maxStockPages = InvAdminShop.maxPages;
                if (InvAdminShop.usePerms) {
                    String permPrefix = Permission.SHOP_STOCK_PREFIX.toString();
                    for (PermissionAttachmentInfo attInfo : event.getPlayer().getEffectivePermissions()) {
                        String perm = attInfo.getPermission();
                        if (perm.startsWith(permPrefix)) {
                            int num;
                            try {
                                num = Integer.parseInt(perm.substring(perm.lastIndexOf(".") + 1));
                            } catch (Exception e) {
                                num = InvAdminShop.maxPages;
                            }
                            if (num > InvAdminShop.permissionMax)
                                maxStockPages = InvAdminShop.permissionMax;
                            else if (num > 0)
                                maxStockPages = num;
                            else
                                maxStockPages = InvAdminShop.maxPages;
                        }
                    }
                }
                inv.setMaxPages(maxStockPages);
                inv.setPag(0);
                inv.open(event.getPlayer());
            }
            return;
        }
        if (multipleStockBlocks && stockEnabled) {
            for (String stockBlocks : multipleStockBlock) {
                Material stockListBlocks = Material.matchMaterial(stockBlocks);
                if (stockListBlocks != null && block.getType().equals(stockListBlocks)) {
                    boolean isShopLoc = true;
                    if (!isShopLoc || event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getPlayer().isSneaking())
                        return;
                    event.setCancelled(true);
                    if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                        if (Shop.getNumShops(event.getPlayer().getUniqueId()) < 1 && noShopNoStock) {
                            event.getPlayer().sendMessage(Messages.NO_SHOP_STOCK.toString());
                            return;
                        }
                        if (stockRangeLimit > 0)
                            if (!Shop.checkShopDistanceFromStockBlock(event.getClickedBlock().getLocation(), event.getPlayer().getUniqueId())) {
                                event.getPlayer().sendMessage(Messages.SHOP_FAR.toString());
                                return;
                            }
                        if (InvStock.inShopInv.containsValue(event.getPlayer().getUniqueId())) {
                            event.getPlayer().sendMessage(Messages.SHOP_BUSY.toString());
                            return;
                        } else {
                            InvStock.inShopInv.put(event.getPlayer(), event.getPlayer().getUniqueId());
                        }
                        InvStock inv = InvStock.getInvStock(event.getPlayer().getUniqueId());
                        int maxStockPages = InvAdminShop.maxPages;
                        if (InvAdminShop.usePerms) {
                            String permPrefix = Permission.SHOP_STOCK_PREFIX.toString();
                            for (PermissionAttachmentInfo attInfo : event.getPlayer().getEffectivePermissions()) {
                                String perm = attInfo.getPermission();
                                if (perm.startsWith(permPrefix)) {
                                    int num;
                                    try {
                                        num = Integer.parseInt(perm.substring(perm.lastIndexOf(".") + 1));
                                    } catch (Exception e) {
                                        num = InvAdminShop.maxPages;
                                    }
                                    if (num > InvAdminShop.permissionMax)
                                        maxStockPages = InvAdminShop.permissionMax;
                                    else if (num > 0)
                                        maxStockPages = num;
                                    else
                                        maxStockPages = InvAdminShop.maxPages;
                                }
                            }
                        }
                        inv.setMaxPages(maxStockPages);
                        inv.setPag(0);
                        inv.open(event.getPlayer());
                    }
                    return;
                }
            }
        }
    }

    @EventHandler
    public void shopSignBreak(BlockBreakEvent event) {
        if (placeFrameSign && shopEnabled) {
            Location attachedBlock;
            if (event.getBlock().getBlockData() instanceof WallSign wallSign) {
				BlockFace blockFace = wallSign.getFacing();
                Block block = event.getBlock().getRelative(blockFace.getOppositeFace());
                attachedBlock = block.getLocation();
            } else if (event.getBlock().getBlockData() instanceof org.bukkit.block.data.type.Sign)
                attachedBlock = event.getBlock().getLocation().subtract(0, 1, 0);
            else
                return;
            Optional<Shop> shop = Shop.getShopByLocation(attachedBlock);
            if (!shop.isPresent() || shop.get().isOwner(event.getPlayer().getUniqueId()) || event.getPlayer().hasPermission(Permission.SHOP_ADMIN.toString())) {
			}
            else
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void protectFromWitherDamage(EntityChangeBlockEvent event) {
        if (protectShopFromExplosion && shopEnabled && event.getEntity() instanceof Wither && checkShopLoc(event.getBlock().getLocation()))
            event.setCancelled(true);
    }

    @EventHandler
    public void itemFrameItem(EntityDamageByEntityEvent event) {
        if (placeFrameSign && shopEnabled && (event.getEntity() instanceof ItemFrame || event.getEntity() instanceof GlowItemFrame)) {
            BlockFace blockFace = event.getEntity().getFacing();
            Block attachedBlock = event.getEntity().getLocation().getBlock().getRelative(blockFace.getOppositeFace());
            Optional<Shop> shop = Shop.getShopByLocation(attachedBlock.getLocation());
            if (!shop.isPresent() || shop.get().isOwner(event.getDamager().getUniqueId()) || event.getDamager().hasPermission(Permission.SHOP_ADMIN.toString())) {
			}
            else
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void entityExplosion(EntityExplodeEvent event) {
        if (protectShopFromExplosion && shopEnabled) {
            if (shopBlk == null) {
                try {
                    shopBlk = Material.matchMaterial(shopBlock.split("minecraft:")[1].toUpperCase());
                } catch (Exception e) {
                    shopBlk = Material.BARREL;
                }
            }
            if (multipleShopBlocks) {
                for (String shopBlocks : multipleShopBlock) {
                    Material shopListBlocks = Material.matchMaterial(shopBlocks);
                    if (shopListBlocks != null)
                        event.blockList().removeIf(block -> block.getType().equals(shopListBlocks) && checkShopLoc(block.getLocation()));
                }
            }
            event.blockList().removeIf(block -> block.getType().equals(shopBlk) && checkShopLoc(block.getLocation()));
        }
    }

    @EventHandler
    public void HangingBreakItemFrameEntity(HangingBreakByEntityEvent event) {
        if (placeFrameSign && shopEnabled && (event.getEntity() instanceof ItemFrame || event.getEntity() instanceof GlowItemFrame)) {
            BlockFace blockFace = event.getEntity().getAttachedFace();
            Block attachedBlock = event.getEntity().getLocation().getBlock().getRelative(blockFace);
            Optional<Shop> shop = Shop.getShopByLocation(attachedBlock.getLocation());
            if (!shop.isPresent() || shop.get().isOwner(event.getRemover().getUniqueId()) || event.getRemover().hasPermission(Permission.SHOP_ADMIN.toString())) {
			}
            else
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void HangingBreakItemFrame(HangingBreakEvent event) {
        if (placeFrameSign && shopEnabled && !event.getCause().equals(HangingBreakEvent.RemoveCause.ENTITY) && (event.getEntity() instanceof ItemFrame || event.getEntity() instanceof GlowItemFrame)) {
            BlockFace blockFace = event.getEntity().getAttachedFace();
            Block attachedBlock = event.getEntity().getLocation().getBlock().getRelative(blockFace);
            Optional<Shop> shop = Shop.getShopByLocation(attachedBlock.getLocation());
            if (!shop.isPresent()) {
			}
            else
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void itemFrameItemRotate(PlayerInteractEntityEvent event) {
        if (placeFrameSign && shopEnabled && (event.getRightClicked() instanceof ItemFrame || event.getRightClicked() instanceof GlowItemFrame)) {
            BlockFace blockFace = event.getRightClicked().getFacing();
            Block attachedBlock = event.getRightClicked().getLocation().getBlock().getRelative(blockFace.getOppositeFace());
            Optional<Shop> shop = Shop.getShopByLocation(attachedBlock.getLocation());
            if (!shop.isPresent() || shop.get().isOwner(event.getPlayer().getUniqueId()) || event.getPlayer().hasPermission(Permission.SHOP_ADMIN.toString())) {
			}
            else
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void shopSoldMessages(PlayerJoinEvent event) {
        if (Shop.stockMessages && soldJoinMessage && Shop.shopMessages.containsKey(event.getPlayer().getUniqueId())) {
            final long delayTime;
            if (soldMessageDelayTime < 1)
                delayTime = 1;
            else
                delayTime = soldMessageDelayTime * 20L;
            final UUID uuid = event.getPlayer().getUniqueId();
            Bukkit.getScheduler().runTaskLaterAsynchronously(GoodTrade.getPlugin(), () -> {
                if (soldOnlyOnFirstConnect && soldListSent.containsKey(event.getPlayer().getUniqueId())) {
                } else if (uuid != null && Bukkit.getPlayer(uuid).isOnline()) {
                    soldListSent.put(uuid, 1);
                    Bukkit.getPlayer(uuid).sendMessage(Messages.SOLD_JOIN_NOTIFY.toString());
                }
            }, delayTime);
        }
    }

    private boolean checkShopLoc(Location location) {
        Optional<Shop> shop = Shop.getShopByLocation(location);
        return shop.isPresent();
    }
}
