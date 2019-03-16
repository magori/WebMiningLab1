package ch.hesso.webminig;

import org.junit.jupiter.api.Test;

class SearchTest {
    @Test
    void testMain_notEmpt() {
        Search.main("my textfield in my JFrame java");
    }

    @Test
    void testMain_empty() {
        Search.main();
    }
}