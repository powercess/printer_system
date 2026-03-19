package com.powercess.printer_system.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powercess.printer_system.config.PaymentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class QixiangPayClient {

    private final PaymentProperties paymentProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QixiangPayResponse createPayment(QixiangPayRequest request) {
        try {
            Map<String, String> params = new TreeMap<>();
            params.put("pid", paymentProperties.pid());
            params.put("type", request.payType());
            params.put("out_trade_no", request.outTradeNo());
            params.put("notify_url", request.notifyUrl());
            params.put("return_url", request.returnUrl() != null ? request.returnUrl() : "");
            params.put("name", request.name());
            params.put("money", request.money().setScale(2, RoundingMode.HALF_UP).toString());

            if (request.clientIp() != null) {
                params.put("clientip", request.clientIp());
            }
            params.put("device", request.device());

            String sign = generateSign(params);
            params.put("sign", sign);
            params.put("sign_type", "MD5");

            MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
            params.forEach(requestParams::add);

            String response = restTemplate.postForObject(
                paymentProperties.apiUrl(),
                requestParams,
                String.class
            );

            log.info("Qixiang payment API response: {}", response);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(response, Map.class);

            if (result.containsKey("code") && !"1".equals(result.get("code").toString())) {
                String msg = result.containsKey("msg") ? result.get("msg").toString() : "支付下单失败";
                return QixiangPayResponse.fail(msg);
            }

            String tradeNo = (String) result.get("trade_no");
            String payUrl = (String) result.get("payurl");
            String qrcode = (String) result.get("qrcode");

            return QixiangPayResponse.success(tradeNo, payUrl, qrcode);

        } catch (Exception e) {
            log.error("Failed to create Qixiang payment", e);
            return QixiangPayResponse.fail("支付下单失败: " + e.getMessage());
        }
    }

    public boolean verifyCallback(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return false;
        }

        String receivedSign = params.get("sign");
        if (receivedSign == null) {
            log.warn("No sign in callback params");
            return false;
        }

        Map<String, String> sortedParams = new TreeMap<>(params);
        sortedParams.remove("sign");
        sortedParams.remove("sign_type");

        String expectedSign = generateSign(sortedParams);
        boolean valid = expectedSign.equalsIgnoreCase(receivedSign);

        if (!valid) {
            log.warn("Sign verification failed. Expected: {}, Received: {}", expectedSign, receivedSign);
        }

        return valid;
    }

    public QixiangQueryResponse queryPayment(String outTradeNo) {
        try {
            Map<String, String> params = new TreeMap<>();
            params.put("pid", paymentProperties.pid());
            params.put("out_trade_no", outTradeNo);

            String sign = generateSign(params);
            params.put("sign", sign);
            params.put("sign_type", "MD5");

            // 正确构建URL参数
            StringBuilder urlBuilder = new StringBuilder(paymentProperties.queryUrl());
            urlBuilder.append("?");
            for (Map.Entry<String, String> entry : params.entrySet()) {
                urlBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                    .append("&");
            }
            String url = urlBuilder.substring(0, urlBuilder.length() - 1); // 移除最后的 &

            log.info("Querying Qixiang payment status: {}", url);
            String response = restTemplate.getForObject(url, String.class);
            log.info("Qixiang query API response: {}", response);

            if (response == null || response.isEmpty()) {
                log.error("Empty response from Qixiang query API");
                return new QixiangQueryResponse(false, null, null, outTradeNo, null, "查询返回空响应");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(response, Map.class);

            Object codeObj = result.get("code");
            if (codeObj == null || !"1".equals(codeObj.toString())) {
                String msg = result.get("msg") != null ? result.get("msg").toString() : "查询失败";
                log.warn("Qixiang query failed: code={}, msg={}", codeObj, msg);
                return new QixiangQueryResponse(false, null, null, outTradeNo, null, msg);
            }

            String tradeStatus = (String) result.get("trade_status");
            String tradeNo = (String) result.get("trade_no");
            String money = result.get("money") != null ? result.get("money").toString() : null;

            log.info("Payment status for {}: tradeStatus={}, tradeNo={}", outTradeNo, tradeStatus, tradeNo);
            return new QixiangQueryResponse(true, tradeStatus, tradeNo, outTradeNo, money, null);

        } catch (Exception e) {
            log.error("Failed to query Qixiang payment", e);
            return new QixiangQueryResponse(false, null, null, outTradeNo, null,
                "查询失败: " + e.getMessage());
        }
    }

    private String generateSign(Map<String, String> params) {
        String signStr = params.entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining("&"));

        signStr += paymentProperties.key();

        return md5(signStr);
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 encryption failed", e);
        }
    }
}