package org.example.rfid.scenario.payment;

import org.example.rfid.scenario.payment.PaymentProcessor;
import org.example.rfid.scenario.payment.PaymentScenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentScenarioTest {

    @Mock
    private PaymentProcessor paymentProcessor;

    @Test
    void constructorRejectsInvalidArguments() {
        assertEquals(
                "paymentProcessor must not be null",
                assertThrows(NullPointerException.class, () -> new PaymentScenario(null, 1)).getMessage()
        );
        assertEquals(
                "maxTransactionsPerTick must be > 0",
                assertThrows(IllegalArgumentException.class, () -> new PaymentScenario(paymentProcessor, 0)).getMessage()
        );
    }

    @Test
    void nameReturnsPayment() {
        PaymentScenario scenario = new PaymentScenario(paymentProcessor, 4);
        assertEquals("payment", scenario.name());
    }

    @Test
    void tickDelegatesToPaymentProcessorWithConfiguredLimit() {
        PaymentScenario scenario = new PaymentScenario(paymentProcessor, 4);

        scenario.tick();

        verify(paymentProcessor, times(1)).tick(4);
    }
}
