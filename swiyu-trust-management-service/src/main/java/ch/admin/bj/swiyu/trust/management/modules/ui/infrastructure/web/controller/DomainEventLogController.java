package ch.admin.bj.swiyu.trust.management.modules.ui.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Expressions.HAS_ROLE_EDITOR_OR_READER;

import ch.admin.bj.swiyu.trust.management.modules.management.api.DomainEventLogDto;
import ch.admin.bj.swiyu.trust.management.modules.management.service.DomainEventService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.SortDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@AllArgsConstructor
@Tag(name = "DomainEventLog")
@RestController
@RequestMapping("/ui-api/domain-event-logs")
public class DomainEventLogController {

    private final DomainEventService domainEventService;

    @PageableAsQueryParam
    @GetMapping("/")
    @PreAuthorize(HAS_ROLE_EDITOR_OR_READER)
    public PagedModel<DomainEventLogDto> getDomainEventLogs(
        @RequestParam(required = false) UUID trustOnboardingTaskId,
        @SortDefault(sort = "triggeredAt", direction = Sort.Direction.DESC) @Parameter(hidden = true) Pageable pageable
    ) {
        return new PagedModel<>(this.domainEventService.getDomainEventLogs(trustOnboardingTaskId, pageable));
    }
}
