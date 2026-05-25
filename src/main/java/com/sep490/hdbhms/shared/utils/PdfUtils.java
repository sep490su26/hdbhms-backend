package com.sep490.hdbhms.shared.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PdfUtils {
    static final PDType1Font TITLE_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    static final PDType1Font HEADER_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    static final PDType1Font BODY_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    static final float TITLE_SIZE = 16;
    static final float HEADER_SIZE = 10;
    static final float BODY_SIZE = 9;
    static final float MARGIN = 50;
    static final float ROW_HEIGHT = 18;
    static final float TABLE_TOP_OFFSET = 9;

    public static <T> byte[] generatePdfTable(
            List<T> data,
            final List<String> headers,
            final Function<T, List<String>> mapper,
            final String title
    ) throws IOException {
        try (
                final PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream();
        ) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            final float pageWidth = page.getMediaBox().getWidth();
            final int numCols = headers.size();
            final float tableWidth = pageWidth - 2 * MARGIN;
            final float colWidth = tableWidth / numCols;
            final float yStart = page.getMediaBox().getHeight() - TABLE_TOP_OFFSET;

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(TITLE_FONT, TITLE_SIZE);
                contentStream.newLineAtOffset(MARGIN, page.getMediaBox().getHeight() - MARGIN);
                contentStream.showText(title);
                contentStream.endText();
            }

            float yPosition = yStart;
            boolean firstPage = true;
            int rowIndex = 0;

            while (rowIndex <= data.size()) {
                if (yPosition < MARGIN) {
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    yPosition = page.getMediaBox().getHeight() - MARGIN;
                    firstPage = false;
                }

                try (
                        PDPageContentStream contentStream = new PDPageContentStream(
                                document,
                                page,
                                PDPageContentStream.AppendMode.APPEND,
                                true
                        )
                ) {
                    if (rowIndex == 0) {
                        drawRow(
                                contentStream,
                                MARGIN,
                                yPosition,
                                colWidth,
                                ROW_HEIGHT,
                                headers,
                                HEADER_FONT,
                                HEADER_SIZE,
                                true
                        );
                        yPosition -= ROW_HEIGHT;
                        rowIndex++;
                    }
                    for (; rowIndex <= data.size(); rowIndex++) {
                        if (yPosition < MARGIN) {
                            break;
                        }
                        T item = data.get(rowIndex - 1);
                        List<String> celValues = mapper.apply(item);
                        drawRow(
                                contentStream,
                                MARGIN,
                                yPosition,
                                colWidth,
                                ROW_HEIGHT,
                                celValues,
                                BODY_FONT,
                                BODY_SIZE,
                                false
                        );
                        yPosition -= ROW_HEIGHT;
                    }
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private static void drawRow(
            PDPageContentStream contentStream,
            final float x,
            final float y,
            final float colWidth,
            final float rowHeight,
            final List<String> values,
            final PDType1Font font,
            final float fontSize,
            final boolean isHeader
    ) throws IOException {
        float textY = y - (rowHeight - fontSize) / 2 - 2;
        for (int idx = 0; idx < values.size(); idx++) {
            float cellX = x + idx * colWidth;
            contentStream.setStrokingColor(0);
            contentStream.addRect(cellX, y - rowHeight, colWidth, rowHeight);
            contentStream.stroke();

            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(cellX + 2, textY);
            String text = values.get(idx);
            if (text != null) {
                contentStream.showText(truncateText(text, font, fontSize, colWidth - 4));
            }
            contentStream.endText();
        }
    }

    private static String truncateText(
            String text,
            final PDType1Font font,
            final float fontSize,
            final float maxWidth
    ) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        if (textWidth <= maxWidth) {
            return text;
        }
        while (!text.isEmpty() && font.getStringWidth(text + "...") / 1000 * fontSize > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "...";
    }
}
