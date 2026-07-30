package com.sep490.hdbhms.identityandaccess.domain.value_objects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GenderTest {
    @Test
    void fromLabelAcceptsVietnameseAndEnumNames() {
        assertEquals(Gender.MALE, Gender.fromLabel("Nam"));
        assertEquals(Gender.FEMALE, Gender.fromLabel("Nữ"));
        assertEquals(Gender.OTHER, Gender.fromLabel("khac"));
        assertEquals(Gender.FEMALE, Gender.fromLabel("FEMALE"));
    }

    @Test
    void toVietnameseLabelReturnsDisplayText() {
        assertEquals("Nam", Gender.MALE.toVietnameseLabel());
        assertEquals("Nữ", Gender.FEMALE.toVietnameseLabel());
        assertEquals("Khác", Gender.OTHER.toVietnameseLabel());
        assertNull(Gender.UNKNOWN.toVietnameseLabel());
    }
}
