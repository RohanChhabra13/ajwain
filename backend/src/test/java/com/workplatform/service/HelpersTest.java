package com.workplatform.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HelpersTest {
    @Test void csvQuotesAndNeutralisesFormulas() {
        assertEquals("\"plain\"", TaskQueryService.csv("plain"));
        assertEquals("\"say \"\"hi\"\"\"", TaskQueryService.csv("say \"hi\""));
        assertEquals("\"'=SUM(A1:A9)\"", TaskQueryService.csv("=SUM(A1:A9)"));
        assertEquals("\"'@cmd\"", TaskQueryService.csv("@cmd"));
        assertEquals("", TaskQueryService.csv(null));
    }

    @Test void attachmentNamesAreStrippedOfPathsAndControlCharacters() {
        assertEquals("passwd", AttachmentService.cleanName("../../etc/passwd"));
        assertEquals("c.txt", AttachmentService.cleanName("a\\b\\c.txt"));
        assertEquals("file", AttachmentService.cleanName(null));
        assertEquals("file", AttachmentService.cleanName("   "));
        assertEquals("a_b.png", AttachmentService.cleanName("a\nb.png"));
    }
}
