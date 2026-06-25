package ch.admin.bj.swiyu.trust.management.modules.management.service;

import ch.admin.bj.swiyu.trust.client.core.business.internal.api.*;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.*;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.*;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.TrustOnboardingSubmissionDocumentListItemDto;
import jakarta.servlet.http.*;
import java.io.*;
import java.net.*;
import java.util.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.data.domain.*;
import org.springframework.data.web.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;
import org.springframework.web.client.*;

@Service
@Slf4j
@AllArgsConstructor
public class TrustOnboardingSubmissionDocumentService {

    private final TrustOnboardingSubmissionApi trustOnboardingSubmissionApi;
    private final TrustOnboardingTaskDomainService taskDomainService;
    private final RestTemplate documentClient;

    public PagedModel<TrustOnboardingSubmissionDocumentListItemDto> getTrustOnboardingSubmissionDocuments(
        Pageable pageable,
        UUID taskId
    ) {
        var task = taskDomainService.getTrustOnboardingTask(taskId);

        var sortParams = pageable
            .getSort()
            .stream()
            .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
            .toList();
        PagedModelTrustOnboardingSubmissionDocumentListItemDto documents;
        try {
            documents = this.trustOnboardingSubmissionApi.listAllDocumentsForTrustOnboarding(
                task.getTrustOnboardingSubmissionId(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sortParams
            );
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw new ExternalSystemProxyException(e);
            } else {
                throw e;
            }
        }
        return new PagedModel<>(
            new PageImpl<>(
                documents
                    .getContent()
                    .stream()
                    .map(TrustOnboardingSubmissionDocumentMapper::toTrustOnboardingSubmissionDocumentListItemDto)
                    .toList(),
                PageRequest.of(
                    Math.toIntExact(documents.getPage().getNumber()),
                    Math.toIntExact(documents.getPage().getSize())
                ),
                documents.getPage().getTotalElements()
            )
        );
    }

    @Transactional(readOnly = true)
    public void getTrustOnboardingSubmissionDocument(UUID taskId, UUID documentId, HttpServletResponse response) {
        var task = taskDomainService.getTrustOnboardingTask(taskId);

        var document = this.trustOnboardingSubmissionApi.getDocumentForTrustOnboarding(
            task.getTrustOnboardingSubmissionId(),
            documentId
        );
        if (document != null) {
            response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"%s\"".formatted(document.getName())
            );
            documentClient.execute(URI.create(document.getDownloadUrl()), HttpMethod.GET, null, rawResponse -> {
                try (InputStream in = rawResponse.getBody()) {
                    in.transferTo(response.getOutputStream());
                }
                return null;
            });
        }
    }
}
