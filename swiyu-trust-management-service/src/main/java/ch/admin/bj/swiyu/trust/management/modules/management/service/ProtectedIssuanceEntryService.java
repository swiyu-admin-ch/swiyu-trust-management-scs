package ch.admin.bj.swiyu.trust.management.modules.management.service;

import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedIssuanceEntryCreateRequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedIssuanceEntryDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedIssuanceEntryFilterDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedIssuanceEntry;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedIssuanceEntryRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.QProtectedIssuanceEntry;
import com.querydsl.core.BooleanBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProtectedIssuanceEntryService {

    private final ProtectedIssuanceEntryRepository protectedIssuanceEntryRepository;
    private final DomainEventService domainEventService;

    private static Supplier<ResourceNotFoundException> protectedIssuanceEntryNotFound(UUID id) {
        return () -> new ResourceNotFoundException("No ProtectedIssuanceEntry found for id %s".formatted(id));
    }

    @Transactional(readOnly = true)
    public ProtectedIssuanceEntryDto getProtectedIssuanceEntry(@NotNull UUID id) {
        var protectedIssuanceEntry = this.protectedIssuanceEntryRepository.findById(id).orElseThrow(
            protectedIssuanceEntryNotFound(id)
        );
        return ProtectedIssuanceEntryMapper.toProtectedIssuanceEntryDto(protectedIssuanceEntry);
    }

    @Transactional(readOnly = true)
    public Page<ProtectedIssuanceEntryDto> listProtectedIssuanceEntries(
        @Valid @NotNull ProtectedIssuanceEntryFilterDto filters,
        @Valid @NotNull Pageable pageable
    ) {
        var q = QProtectedIssuanceEntry.protectedIssuanceEntry;
        BooleanBuilder where = new BooleanBuilder();
        if (filters.vct() != null && !filters.vct().isBlank()) {
            where.and(q.vct.containsIgnoreCase(filters.vct()));
        }
        return this.protectedIssuanceEntryRepository.findAll(where, pageable).map(
            ProtectedIssuanceEntryMapper::toProtectedIssuanceEntryDto
        );
    }

    @Transactional
    public ProtectedIssuanceEntryDto createProtectedIssuanceEntry(
        @Valid @NotNull ProtectedIssuanceEntryCreateRequestDto request,
        String currentUserFullName
    ) {
        var preExistingEntry = this.protectedIssuanceEntryRepository.findByVct(request.vct());
        if (preExistingEntry.isPresent()) {
            throw new IllegalArgumentException(
                "Validation failed: ProtectedIssuanceEntry with given vct '%s' already exists on id %s.".formatted(
                    request.vct(),
                    preExistingEntry.get().getId()
                )
            );
        }
        var protectedIssuanceEntry = new ProtectedIssuanceEntry(request.vct(), Instant.now());
        var savedNonCompliantActor = this.protectedIssuanceEntryRepository.save(protectedIssuanceEntry);
        this.domainEventService.protectedIssuanceEntryAdded(savedNonCompliantActor.getId(), currentUserFullName);
        return ProtectedIssuanceEntryMapper.toProtectedIssuanceEntryDto(savedNonCompliantActor);
    }

    @Transactional
    public void deleteProtectedIssuanceEntry(@Valid @NotNull UUID id, String currentUserFullName) {
        this.protectedIssuanceEntryRepository.deleteById(id);
        this.domainEventService.protectedIssuanceEntryRemoved(id, currentUserFullName);
    }
}
