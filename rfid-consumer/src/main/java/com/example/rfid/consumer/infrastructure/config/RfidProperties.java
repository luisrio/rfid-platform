package com.example.rfid.consumer.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "rfid")
public class RfidProperties {

    private final Mqtt mqtt = new Mqtt();
    private final Filter filter = new Filter();
    private final Topics topics = new Topics();
    private final Monitoring monitoring = new Monitoring();

    public Mqtt getMqtt() {
        return mqtt;
    }

    public Filter getFilter() {
        return filter;
    }

    public Topics getTopics() {
        return topics;
    }

    public Monitoring getMonitoring() {
        return monitoring;
    }

    public static class Mqtt {
        private String url = "tcp://localhost:1883";
        private String clientId = "rfid-consumer-01";
        private String topic = "tienda/#";
        private List<String> topics = new ArrayList<>();
        private int qos = 1;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public List<String> getTopics() {
            return topics;
        }

        public void setTopics(List<String> topics) {
            this.topics = topics;
        }

        public int getQos() {
            return qos;
        }

        public void setQos(int qos) {
            this.qos = qos;
        }
    }

    public static class Filter {
        private int minRssi = -70;
        private long dedupWindowMs = 1000;
        private long paidTtlMinutes = 30;

        public int getMinRssi() {
            return minRssi;
        }

        public void setMinRssi(int minRssi) {
            this.minRssi = minRssi;
        }

        public long getDedupWindowMs() {
            return dedupWindowMs;
        }

        public void setDedupWindowMs(long dedupWindowMs) {
            this.dedupWindowMs = dedupWindowMs;
        }

        public long getPaidTtlMinutes() {
            return paidTtlMinutes;
        }

        public void setPaidTtlMinutes(long paidTtlMinutes) {
            this.paidTtlMinutes = paidTtlMinutes;
        }
    }

    public static class Topics {
        private String cashierPrefix = "tienda/lecturas/caja/";
        private String exitPrefix = "tienda/lecturas/salida/";
        private String fittingPrefix = "tienda/lecturas/probador/";
        private String paymentPrefix = "tienda/pagos/confirmados";
        private String alerts = "tienda/alertas/seguridad";

        public String getCashierPrefix() {
            return cashierPrefix;
        }

        public void setCashierPrefix(String cashierPrefix) {
            this.cashierPrefix = cashierPrefix;
        }

        public String getExitPrefix() {
            return exitPrefix;
        }

        public void setExitPrefix(String exitPrefix) {
            this.exitPrefix = exitPrefix;
        }

        public String getAlerts() {
            return alerts;
        }

        public void setAlerts(String alerts) {
            this.alerts = alerts;
        }

        public String getFittingPrefix() {
            return fittingPrefix;
        }

        public void setFittingPrefix(String fittingPrefix) {
            this.fittingPrefix = fittingPrefix;
        }

        public String getPaymentPrefix() {
            return paymentPrefix;
        }

        public void setPaymentPrefix(String paymentPrefix) {
            this.paymentPrefix = paymentPrefix;
        }
    }

    public static class Monitoring {
        private long logEveryMessages = 200;
        private long fittingSeenTtlMinutes = 1440;

        public long getLogEveryMessages() {
            return logEveryMessages;
        }

        public void setLogEveryMessages(long logEveryMessages) {
            this.logEveryMessages = logEveryMessages;
        }

        public long getFittingSeenTtlMinutes() {
            return fittingSeenTtlMinutes;
        }

        public void setFittingSeenTtlMinutes(long fittingSeenTtlMinutes) {
            this.fittingSeenTtlMinutes = fittingSeenTtlMinutes;
        }
    }
}
