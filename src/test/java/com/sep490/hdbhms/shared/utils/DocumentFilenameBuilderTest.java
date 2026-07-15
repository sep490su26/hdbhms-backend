package com.sep490.hdbhms.shared.utils;

import com.sep490.hdbhms.shared.utils.DocumentFilenameBuilder.DocumentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentFilenameBuilderTest {

    @Test
    void normalizesVietnameseTenantName() {
        assertEquals(
                "P101_Nguyen-Van-A_HDC_29_06_2026.pdf",
                DocumentFilenameBuilder.build("101", "Nguyễn Văn A", DocumentType.HDC, LocalDate.of(2026, 6, 29))
        );
    }

    @Test
    void removesUnsafeCharactersAndNonPrintableAscii() {
        assertEquals(
                "P101_NguyenVanA_HDT_29_06_2026.pdf",
                DocumentFilenameBuilder.build("101", "Nguyen/Van:A*?\u0001", DocumentType.HDT, LocalDate.of(2026, 6, 29))
        );
    }

    @Test
    void fallsBackForNullInputs() {
        assertEquals(
                "PPhong_Khach-Thue_BBBG_ngay-chua-xac-dinh.pdf",
                DocumentFilenameBuilder.build(null, "   ", DocumentType.BBBG, null)
        );
    }

    @Test
    void truncatesLongTenantNameAfterNormalize() {
        String filename = DocumentFilenameBuilder.build(
                "101",
                "Nguyen Van A Nguyen Van A Nguyen Van A Nguyen Van A",
                DocumentType.HDC,
                LocalDate.of(2026, 6, 29)
        );

        assertEquals("P101_Nguyen-Van-A-Nguyen-Van-A-Nguyen-Van-A-N_HDC_29_06_2026.pdf", filename);
    }

    @Test
    void buildsRfc5987AttachmentHeader() {
        assertEquals(
                "attachment; filename*=UTF-8''P101_Nguyen-Van-A_HDC_29_06_2026.pdf",
                DocumentFilenameBuilder.attachmentContentDisposition("P101_Nguyen-Van-A_HDC_29_06_2026.pdf")
        );
    }
}
