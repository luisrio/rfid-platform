package org.example.rfid;

import org.example.rfid.bootstrap.ProducerApplication;

public final class App {

    private App() {
    }

    public static void main(String[] args) {
        new ProducerApplication().run(args);
    }
}
