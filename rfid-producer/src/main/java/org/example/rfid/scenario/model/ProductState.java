package org.example.rfid.scenario.model;

public final class ProductState {
    public final String epc;
    public Stage stage;
    public int remainingStageTicks;

    public ProductState(String epc, Stage stage, int remainingStageTicks) {
        this.epc = epc;
        this.stage = stage;
        this.remainingStageTicks = remainingStageTicks;
    }
}
