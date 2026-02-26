package com.example.rfid.consumer.support;

import java.util.List;
import java.util.stream.Collectors;

public final class TestPayloadFactory {

    private TestPayloadFactory() {
    }

    public static String rfidEvent(String epc, String readerId, int rssi, long timestamp) {
        return String.format(
                "{\"epc\":%s,\"readerId\":%s,\"rssi\":%d,\"timestamp\":%d}",
                jsonStringOrNull(epc),
                jsonStringOrNull(readerId),
                rssi,
                timestamp
        );
    }

    public static String paymentConfirmation(String transactionId, String cashierReaderId, long timestamp, List<String> paidEpcs) {
        String paidEpcsJson = paidEpcs == null
                ? "null"
                : "[" + paidEpcs.stream().map(TestPayloadFactory::jsonStringOrNull).collect(Collectors.joining(",")) + "]";

        return String.format(
                "{\"transactionId\":%s,\"cashierReaderId\":%s,\"timestamp\":%d,\"paidEpcs\":%s}",
                jsonStringOrNull(transactionId),
                jsonStringOrNull(cashierReaderId),
                timestamp,
                paidEpcsJson
        );
    }

    public static String securityAlert(
            String epc,
            String readerId,
            int rssi,
            long sourceTimestamp,
            String sourceTopic,
            String publishedAt
    ) {
        return String.format(
                "{\"type\":\"SECURITY_ALERT\",\"reason\":\"UNPAID_EPC_AT_EXIT\",\"epc\":%s,\"readerId\":%s,\"rssi\":%d,\"sourceTimestamp\":%d,\"sourceTopic\":%s,\"publishedAt\":%s}",
                jsonStringOrNull(epc),
                jsonStringOrNull(readerId),
                rssi,
                sourceTimestamp,
                jsonStringOrNull(sourceTopic),
                jsonStringOrNull(publishedAt)
        );
    }

    public static String invalidJson() {
        return "{invalid";
    }

    public static String jsonWithField(String field, String rawJsonValue) {
        return "{\"" + escape(field) + "\":" + rawJsonValue + "}";
    }

    private static String jsonStringOrNull(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + escape(value) + "\"";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
