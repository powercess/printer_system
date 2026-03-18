package com.powercess.printer_system.payment;

public record QixiangQueryResponse(
    boolean success,
    String tradeStatus,
    String tradeNo,
    String outTradeNo,
    String money,
    String msg
) {
    public static final String TRADE_SUCCESS = "TRADE_SUCCESS";
    public static final String TRADE_PENDING = "TRADE_PENDING";

    public boolean isPaid() {
        return TRADE_SUCCESS.equals(tradeStatus);
    }
}