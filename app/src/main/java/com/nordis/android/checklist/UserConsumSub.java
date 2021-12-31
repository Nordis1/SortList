package com.nordis.android.checklist;

public class UserConsumSub {
    String PurchaseTime;
    String OrderId;
    String PurchaseToken;
    String purchasePrice;

    public UserConsumSub(String purchaseTime, String orderId, String purchaseToken, String purchasePrice) {
        PurchaseTime = purchaseTime;
        OrderId = orderId;
        PurchaseToken = purchaseToken;
        this.purchasePrice = purchasePrice;
    }

    public String getPurchaseTime() {
        return PurchaseTime;
    }

    public void setPurchaseTime(String purchaseTime) {
        PurchaseTime = purchaseTime;
    }

    public String getOrderId() {
        return OrderId;
    }

    public void setOrderId(String orderId) {
        OrderId = orderId;
    }

    public String getPurchaseToken() {
        return PurchaseToken;
    }

    public void setPurchaseToken(String purchaseToken) {
        PurchaseToken = purchaseToken;
    }

    public String getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(String purchasePrice) {
        this.purchasePrice = purchasePrice;
    }
}
