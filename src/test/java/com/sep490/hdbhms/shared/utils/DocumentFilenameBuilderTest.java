package com.sep490.hdbhms.shared.utils;

import com.sep490.hdbhms.shared.utils.DocumentFilenameBuilder.DocumentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentFilenameBuilderTest {

    @Test
    void buildsTypeRoomDateFilename() {
        assertEquals(
                "HDC_P101_29_06_2026.pdf",
                DocumentFilenameBuilder.build("101", "Nguyễn Văn A", DocumentType.HDC, LocalDate.of(2026, 6, 29))
        );
    }

    @Test
    void removesUnsafeCharactersAndNonPrintableAscii() {
        assertEquals(
                "HDT_P101_29_06_2026.pdf",
                DocumentFilenameBuilder.build("101", "Nguyen/Van:A*?\u0001", DocumentType.HDT, LocalDate.of(2026, 6, 29))
        );
    }

    @Test
    void fallsBackForNullInputs() {
        assertEquals(
                "BBBG_Phong-X_Chua-Ro-Ngay.pdf",
                DocumentFilenameBuilder.build(null, "   ", DocumentType.BBBG, null)
        );
    }

    @Test
    void doesNotDoubleExistingRoomPrefix() {
        String filename = DocumentFilenameBuilder.build(
                "P101",
                "Nguyen Van A Nguyen Van A Nguyen Van A Nguyen Van A",
                DocumentType.HDC,
                LocalDate.of(2026, 6, 29)
        );

        assertEquals("HDC_P101_29_06_2026.pdf", filename);
    }

    @Test
    void buildsRfc5987AttachmentHeader() {
        assertEquals(
                "attachment; filename*=UTF-8''HDC_P101_29_06_2026.pdf",
                DocumentFilenameBuilder.attachmentContentDisposition("HDC_P101_29_06_2026.pdf")
        );
    }
}
