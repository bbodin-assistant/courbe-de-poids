package fr.bbodin.courbedepoids;

import static org.junit.Assert.*;
import org.junit.Test;

public class LoggingTypeRegistryTest {
    @Test public void acceptsSimpleAndCustomTypeIds() {
        assertTrue(LoggingTypeRegistry.isValidTypeId("poop"));
        assertTrue(LoggingTypeRegistry.isValidTypeId("daily_note_2"));
        assertFalse(LoggingTypeRegistry.isValidTypeId(""));
        assertFalse(LoggingTypeRegistry.isValidTypeId("Poop"));
        assertFalse(LoggingTypeRegistry.isValidTypeId("poop-event"));
        assertFalse(LoggingTypeRegistry.isValidTypeId(null));
        assertFalse(LoggingTypeRegistry.isValidTypeId("a".repeat(41)));
    }

    @Test public void acceptsAllSupportedDynamicFieldTypes() {
        assertTrue(LoggingTypeRegistry.isSupportedFieldType("text"));
        assertTrue(LoggingTypeRegistry.isSupportedFieldType("number"));
        assertTrue(LoggingTypeRegistry.isSupportedFieldType("select"));
        assertTrue(LoggingTypeRegistry.isSupportedFieldType("boolean"));
        assertTrue(LoggingTypeRegistry.isSupportedFieldType("choice_grid"));
        assertFalse(LoggingTypeRegistry.isSupportedFieldType("date"));
        assertFalse(LoggingTypeRegistry.isSupportedFieldType(null));
    }

    @Test public void validatesFieldDefinitionsAndSelectOptionsRequirement() {
        assertTrue(LoggingTypeRegistry.isValidFieldDefinition("notes", "Notes", "text", false));
        assertTrue(LoggingTypeRegistry.isValidFieldDefinition("consistency", "Consistance", "select", true));
        assertTrue(LoggingTypeRegistry.isValidFieldDefinition("amount_ml", "Quantité d’eau", "choice_grid", true));
        assertFalse(LoggingTypeRegistry.isValidFieldDefinition("consistency", "Consistance", "select", false));
        assertFalse(LoggingTypeRegistry.isValidFieldDefinition("amount_ml", "Quantité d’eau", "choice_grid", false));
        assertFalse(LoggingTypeRegistry.isValidFieldDefinition("", "Notes", "text", false));
        assertFalse(LoggingTypeRegistry.isValidFieldDefinition("notes", " ", "text", false));
        assertFalse(LoggingTypeRegistry.isValidFieldDefinition("notes", "Notes", "unknown", true));
    }
}
