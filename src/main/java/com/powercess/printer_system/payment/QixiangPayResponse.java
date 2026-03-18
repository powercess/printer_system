package com.powercess.printer_system.payment;

public record QixiangPayResponse(
    boolean success,
    String tradeNo,
    String payUrl,
    String qrcode,
    String msg
) {
    public static QixiangPayResponse success(String tradeNo, String payUrl, String qrcode) {
        return new QixiangPayResponse(true, tradeNo, payUrl, qrcode, null);
    }

    public static QixiangPayResponse fail(String msg) {
        return new QixiangPayResponse(false, null, null, null, msg);
    }
}