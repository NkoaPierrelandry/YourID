package com.yourid.api.stores.enums;

public enum StorePlan {
    STARTER(25),
    GROWTH(100),
    SCALE(-1);

    private final int maxProducts;

    StorePlan(int maxProducts) {
        this.maxProducts = maxProducts;
    }

    public int getMaxProducts() {
        return maxProducts;
    }

    public boolean isUnlimited() {
        return maxProducts < 0;
    }
}
