package fr.bbodin.courbedepoids;

import static org.junit.Assert.*;
import org.junit.Test;

public class WeightDatabaseValidationTest {
    @Test public void validatesGenericEventMetadataAndPayload() {
        assertTrue(WeightDatabase.isValidGenericEvent("poop", "2026-10-12", 0L, "{}"));
        assertTrue(WeightDatabase.isValidGenericEvent("daily_note_2", "2026-01-01", 123L, "{\"mood\":\"good\"}"));
        assertFalse(WeightDatabase.isValidGenericEvent(null, "2026-10-12", 0L, "{}"));
        assertFalse(WeightDatabase.isValidGenericEvent("Poop", "2026-10-12", 0L, "{}"));
        assertFalse(WeightDatabase.isValidGenericEvent("poop", "12-10-2026", 0L, "{}"));
        assertFalse(WeightDatabase.isValidGenericEvent("poop", "2026-10-12", -1L, "{}"));
        assertFalse(WeightDatabase.isValidGenericEvent("poop", "2026-10-12", 0L, null));
    }

    @Test public void existingMeasurementAndEventValidationStillWorks() {
        assertTrue(WeightDatabase.isValidWeight(75.5));
        assertFalse(WeightDatabase.isValidWeight(0.0));
        assertTrue(WeightDatabase.isValidWaterAmount(250));
        assertFalse(WeightDatabase.isValidWaterAmount(0));
        assertTrue(WeightDatabase.isValidSport("Course", 30, 5.0));
        assertFalse(WeightDatabase.isValidSport("Course", 0, 5.0));
    }
}
