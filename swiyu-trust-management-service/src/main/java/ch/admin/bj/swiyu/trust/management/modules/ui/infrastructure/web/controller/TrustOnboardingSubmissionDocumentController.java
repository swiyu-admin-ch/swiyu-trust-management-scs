package ch.admin.bj.swiyu.trust.management.modules.ui.infrastructure.web.controller;

import ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRoles;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustOnboardingSubmissionDocumentService;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.TrustOnboardingSubmissionDocumentListItemDto;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.SortDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@AllArgsConstructor
@Tag(name = "TrustOnboardingDocument")
@RestController
@RequestMapping("/ui-api/documents")
@PreAuthorize("isAuthenticated()")
public class TrustOnboardingSubmissionDocumentController {

    private final TrustOnboardingSubmissionDocumentService trustOnboardingSubmissionDocumentService;

    @GetMapping("/")
    @PreAuthorize("hasAnyRole('" + UserRoles.READER + "', '" + UserRoles.EDITOR + "')")
    @PageableAsQueryParam
    public PagedModel<TrustOnboardingSubmissionDocumentListItemDto> getTrustOnboardingSubmissionDocuments(
        @SortDefault(sort = "updatedAt", direction = Sort.Direction.DESC) @Parameter(
            hidden = true
        ) final Pageable pageable,
        @RequestParam UUID taskId
    ) {
        return this.trustOnboardingSubmissionDocumentService.getTrustOnboardingSubmissionDocuments(pageable, taskId);
    }

    @GetMapping(value = "/{taskId}/{documentId}")
    @PreAuthorize("hasAnyRole('" + UserRoles.READER + "', '" + UserRoles.EDITOR + "')")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "File download",
                content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
            ),
        }
    )
    public void getTrustOnboardingSubmissionDocument(
        @PathVariable UUID taskId,
        @PathVariable UUID documentId,
        HttpServletResponse response
    ) {
        this.trustOnboardingSubmissionDocumentService.getTrustOnboardingSubmissionDocument(
            taskId,
            documentId,
            response
        );
    }
}
