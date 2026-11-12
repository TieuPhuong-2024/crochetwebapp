package org.crochet.service;

import org.crochet.controller.UploadController;
import org.crochet.exception.ApiExceptionHandler;
import org.crochet.exception.UploadStatusExceptionHandler;
import org.crochet.payload.response.PresignedUploadResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.crochet.service.UploadTestSupport.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UploadControllerTest {
    private AnnotationConfigApplicationContext context;
    private UploadReservationService service;
    private MockMvc mvc;
    private static final String REQUEST = """
            {"module":"blogs","category":"images","fileName":"a.png","contentType":"image/png","size":100}
            """;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(MethodSecurityOnly.class);
        service = context.getBean(UploadReservationService.class);
        mvc = MockMvcBuilders.standaloneSetup(context.getBean(UploadController.class))
                .setControllerAdvice(new UploadStatusExceptionHandler(), new ApiExceptionHandler()).build();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void unauthenticatedCannotReserveDespiteGlobalPermitAll() throws Exception {
        mvc.perform(post("/api/v1/uploads/presigned").contentType("application/json").content(REQUEST))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void authenticatedRequestUsesResponseDataContract() throws Exception {
        authenticate("owner", "ROLE_USER");
        when(service.reserve(any())).thenReturn(new PresignedUploadResponse(KEY, CDN + "/" + KEY,
                "https://signed.example.test", NOW.plusSeconds(300)));
        mvc.perform(post("/api/v1/uploads/presigned").contentType("application/json").content(REQUEST))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.objectKey").value(KEY))
                .andExpect(jsonPath("$.data.publicUrl").value(CDN + "/" + KEY))
                .andExpect(jsonPath("$.data.signedUrl").value("https://signed.example.test"));
    }

    @Test
    void lifecycleStatusDoesNotFallThroughToGeneric500Handler() throws Exception {
        authenticate("owner", "ROLE_USER");
        when(service.reserve(any())).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access is required"));
        mvc.perform(post("/api/v1/uploads/presigned").contentType("application/json").content(REQUEST))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void invalidMetadataIsRejectedBeforeService() throws Exception {
        authenticate("owner", "ROLE_USER");
        mvc.perform(post("/api/v1/uploads/presigned").contentType("application/json").content(REQUEST.replace("100", "0")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurityOnly {
        @Bean
        UploadReservationService reservations() {
            return mock(UploadReservationService.class);
        }

        @Bean
        UploadController controller(UploadReservationService reservations) {
            return new UploadController(reservations);
        }
    }
}
