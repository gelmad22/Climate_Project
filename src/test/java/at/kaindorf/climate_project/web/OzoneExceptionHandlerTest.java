package at.kaindorf.climate_project.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OzoneExceptionHandlerTest {

    private final OzoneExceptionHandler exceptionHandler = new OzoneExceptionHandler();

    @Test
    void handleIllegalArgumentExceptionCreatesBadRequestBody() {
        Map<String, Object> response = exceptionHandler.handleIllegalArgumentException(
                new IllegalArgumentException("From must be before to")
        );

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.get("status"));
        assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), response.get("error"));
        assertEquals("From must be before to", response.get("message"));
    }
}
