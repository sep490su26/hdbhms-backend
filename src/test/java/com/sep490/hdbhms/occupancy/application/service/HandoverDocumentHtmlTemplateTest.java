package com.sep490.hdbhms.occupancy.application.service;

import com.lowagie.text.pdf.BaseFont;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandoverDocumentHtmlTemplateTest {

    @Test
    void rendersUpdatedWordStructureAndDynamicEquipmentRowsToPdf() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        Context context = new Context();
        context.setVariables(Map.of(
                "roomNumber", "P403",
                "roomFloorNumber", "4",
                "equipmentRows", List.of(Map.of(
                        "name", "Điều hòa",
                        "description", "APPLIANCE: Hoạt động bình thường",
                        "quantity", 1,
                        "condition", "Tốt"
                ))
        ));

        String html = templateEngine.process("contractTemplates/html/handover_contract_template", context);
        String normalizedHtml = html.replaceAll("\\s+", " ").trim();

        assertTrue(normalizedHtml.contains("PHỤ LỤC HỢP ĐỒNG"));
        assertTrue(normalizedHtml.contains("Danh mục nội thất bàn giao"));
        assertTrue(normalizedHtml.contains("P403"));
        assertTrue(normalizedHtml.contains("4"));
        assertTrue(normalizedHtml.contains("APPLIANCE: Hoạt động bình thường"));
        assertTrue(normalizedHtml.contains("NỘI QUY CỘNG ĐỒNG TẠI TÒA NHÀ"));
        assertFalse(html.contains("th:each"));

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.getFontResolver().addFont(
                    getClass().getClassLoader().getResource("fonts/times.ttf").toExternalForm(),
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED
            );
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(output);
            assertTrue(output.toByteArray().length > 1000);
        } catch (Exception exception) {
            throw new AssertionError("Handover HTML must be renderable as PDF", exception);
        }
    }
}
