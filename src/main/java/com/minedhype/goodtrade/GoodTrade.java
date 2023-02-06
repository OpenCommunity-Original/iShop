package com.minedhype.goodtrade;

import com.minedhype.goodtrade.gui.GUIEvent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GoodTrade extends JavaPlugin {
    File configFile;
    public static FileConfiguration config;
    private static BukkitTask expiredTask, saveTask, tickTask;
    private static Connection connection = null;
    private static Economy economy = null;
    private static String chainConnect;

    @Override
    public void onEnable() {
        chainConnect = "jdbc:sqlite:" + getDataFolder().getAbsolutePath() + "/shops.db";
        this.setupEconomy();
        this.createConfig();
        if (config.getString("shopBlock") == null) {
            config.set("shopBlock", "minecraft:barrel");
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[GoodTrade] " + Messages.NO_SHOP_BLOCK);
        }
        if (config.getString("stockBlock") == null) {
            config.set("stockBlock", "minecraft:composter");
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[GoodTrade] " + Messages.NO_STOCK_BLOCK);
        }
        getServer().getPluginManager().registerEvents(new EventShop(), this);
        getServer().getPluginManager().registerEvents(new GUIEvent(), this);
        getCommand("goodtrade").setExecutor(new CommandShop());
        int delayTime;
        int saveDatabaseTime;
        try {
            Class.forName("org.sqlite.JDBC");
            delayTime = config.getInt("shopsDatabaseLoadDelay");
        } catch (Exception e) {
            delayTime = 0;
        }
        if (delayTime < 1)
            delayTime = 1;
        else
            delayTime *= 20;
        try {
            saveDatabaseTime = config.getInt("saveDatabase");
        } catch (Exception e) {
            saveDatabaseTime = 15;
        }
        if (saveDatabaseTime < 5)
            saveDatabaseTime = 5;
        (saveDatabaseTime) *= 60;
        (saveDatabaseTime) *= 20;
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                connection = DriverManager.getConnection(chainConnect);
                this.createTables();
                Shop.loadData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, delayTime);
        expiredTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, Shop::expiredShops, delayTime + 9, 20000);
        saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                Shop.saveData(false);
            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "[GoodTrade] Warning: Tried saving to database while being modified at the same time. Saving will continue and try again later, but will also save to database safely upon server shutdown.");
            }
        }, delayTime + 1200, saveDatabaseTime);
        if (Shop.shopEnabled && Shop.particleEffects)
            tickTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, Shop::tickShops, delayTime + 250, 50);
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, Shop::getPlayersShopList, delayTime + 160);
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, Shop::removeEmptyShopTrade, delayTime + 100);
    }

    @Override
    public void onDisable() {
        expiredTask.cancel();
        saveTask.cancel();
        tickTask.cancel();
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[GoodTrade] Safely saving shops & stock items to database, please wait & do not kill server process...");
        Shop.saveData(true);
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[GoodTrade] Saving complete!");
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void createTables() {
        PreparedStatement[] stmts = new PreparedStatement[]{};
        try {
            stmts = new PreparedStatement[]{
                    connection.prepareStatement("CREATE TABLE IF NOT EXISTS zooMercaTiendas(id INTEGER PRIMARY KEY autoincrement, location varchar(64), owner varchar(64));"),
                    connection.prepareStatement("CREATE TABLE IF NOT EXISTS zooMercaTiendasFilas(itemIn text, itemOut text, idTienda INTEGER);"),
                    connection.prepareStatement("CREATE TABLE IF NOT EXISTS zooMercaStocks(owner varchar(64), items JSON);")
            };
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (PreparedStatement stmt : stmts) {
            try {
                stmt.execute();
                stmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        List<PreparedStatement> stmtsPatches = new ArrayList<>();
        try {
            stmtsPatches.add(connection.prepareStatement("ALTER TABLE zooMercaTiendasFilas ADD COLUMN itemIn2 text NULL DEFAULT 'v: 2580\ntype: AIR\namount: 0' "));
            stmtsPatches.add(connection.prepareStatement("ALTER TABLE zooMercaTiendasFilas ADD COLUMN itemOut2 text NULL DEFAULT 'v: 2580\ntype: AIR\namount: 0' "));
            stmtsPatches.add(connection.prepareStatement("ALTER TABLE zooMercaTiendasFilas ADD COLUMN broadcast BOOLEAN DEFAULT 0"));
            stmtsPatches.add(connection.prepareStatement("ALTER TABLE zooMercaStocks ADD COLUMN pag INTEGER DEFAULT 0"));
            stmtsPatches.add(connection.prepareStatement("ALTER TABLE zooMercaTiendas ADD COLUMN admin BOOLEAN DEFAULT FALSE;"));
        } catch (Exception ignored) {
        }
        for (PreparedStatement stmtsPatch : stmtsPatches) {
            try {
                stmtsPatch.execute();
                stmtsPatch.close();
            } catch (Exception ignored) {
            }
        }
    }

    public static Connection getConnection() {
        checkConnection();
        return connection;
    }

    public static void checkConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(0))
                connection = DriverManager.getConnection(chainConnect);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null)
            return;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null)
            return;
        economy = rsp.getProvider();
    }

    public static Optional<Economy> getEconomy() {
        return Optional.ofNullable(economy);
    }

    public void createConfig() {
        this.configFile = new File(getDataFolder(), "config.yml");
        if (!this.configFile.exists()) {
            this.configFile.getParentFile().mkdirs();
            saveResource("config.yml", false);
        }
        config = new YamlConfiguration();
        try {
            config.load(this.configFile);
            String ver = config.getString("configVersion");
			if (ver.equals("3.7")) {
			}
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static GoodTrade getPlugin() {
        return GoodTrade.getPlugin(GoodTrade.class);
    }
}
