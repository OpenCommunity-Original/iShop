package com.minedhype.goodtrade;

public enum Permission {
    SHOP_ADMIN("goodtrade.admin"),
    SHOP_CREATE("goodtrade.create"),
    SHOP_LIMIT_BYPASS("goodtrade.create.limit.bypass"),
    SHOP_LIMIT_PREFIX("goodtrade.create.limit."),
    SHOP_LIST("goodtrade.list"),
    SHOP_REMOTEMANAGE("goodtrade.remotemanage"),
    SHOP_REMOTESHOPPING("goodtrade.remoteshopping"),
    SHOP_SHOPS("goodtrade.shops"),
    SHOP_STOCK("goodtrade.stock"),
    SHOP_STOCK_PREFIX("goodtrade.pages.");

    private final String perm;

    Permission(String perms) {
        this.perm = perms;
    }

    @Override
    public String toString() {
        return perm;
    }
}
