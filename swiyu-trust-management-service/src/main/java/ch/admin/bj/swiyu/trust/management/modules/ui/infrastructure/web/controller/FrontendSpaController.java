package ch.admin.bj.swiyu.trust.management.modules.ui.infrastructure.web.controller;

import io.swagger.v3.oas.annotations.tags.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "FrontendSpaController",
    description = """
    Provides the SPA index entry file on all relevant ui paths."""
)
@Controller
@RequiredArgsConstructor
@Slf4j
public class FrontendSpaController {

    private static final ClassPathResource index = new ClassPathResource("/static/index.html");

    @GetMapping(path = { "", "/", "/ui", "/ui/**" })
    public ResponseEntity<Object> forwardAngularPaths() {
        return new ResponseEntity<>(index, HttpStatus.OK);
    }
}
