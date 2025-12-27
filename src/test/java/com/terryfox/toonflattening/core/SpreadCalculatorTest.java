package com.terryfox.toonflattening.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class SpreadCalculatorTest {

    private SpreadCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new SpreadCalculator();
    }

    @Test
    void calculateNewSpread_addsIncrement() {
        float newSpread = calculator.calculateNewSpread(1.8f, 0.8f, 6.0f);

        assertEquals(2.6f, newSpread, 0.001f);
    }

    @Test
    void calculateNewSpread_capsAtMaximum() {
        float newSpread = calculator.calculateNewSpread(5.5f, 0.8f, 6.0f);

        assertEquals(6.0f, newSpread, 0.001f);
    }

    @Test
    void calculateNewSpread_alreadyAtMaxReturnsMax() {
        float newSpread = calculator.calculateNewSpread(6.0f, 0.8f, 6.0f);

        assertEquals(6.0f, newSpread, 0.001f);
    }

    @Test
    void calculateNewSpread_exceedsMaxClamps() {
        float newSpread = calculator.calculateNewSpread(5.8f, 1.0f, 6.0f);

        assertEquals(6.0f, newSpread, 0.001f);
    }

    @ParameterizedTest
    @CsvSource({
        "0.0, 1.8, 6.0, 1.8",
        "1.8, 0.8, 6.0, 2.6",
        "2.6, 0.8, 6.0, 3.4",
        "5.2, 0.8, 6.0, 6.0",
        "0.0, 2.0, 4.0, 2.0",
        "3.5, 1.5, 4.0, 4.0"
    })
    void calculateNewSpread_multipleScenarios(float current, float increment, float max, float expected) {
        float result = calculator.calculateNewSpread(current, increment, max);

        assertEquals(expected, result, 0.001f);
    }

    @Test
    void calculateNewSpread_zeroIncrementReturnsOriginal() {
        float newSpread = calculator.calculateNewSpread(2.5f, 0.0f, 6.0f);

        assertEquals(2.5f, newSpread, 0.001f);
    }

    @Test
    void calculateNewSpread_negativeIncrementNotSupported() {
        // Per spec, spread only increases, negative increment should not reduce
        float newSpread = calculator.calculateNewSpread(3.0f, -0.5f, 6.0f);

        assertEquals(2.5f, newSpread, 0.001f);
    }
}
