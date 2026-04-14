package ch.admin.bj.swiyu.trust.management.modules.management.service;

import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.management.api.NonCompliantActorDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.NonCompliantActorFilterDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.NonCompliantActorRequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantActor;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantActorRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantReasonText;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.QNonCompliantActor;
import com.querydsl.core.BooleanBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NonCompliantActorService {

    private final NonCompliantActorRepository nonCompliantActorRepository;
    private final DomainEventService domainEventService;

    @Transactional(readOnly = true)
    public NonCompliantActorDto getNonCompliantActor(@Valid @NotNull UUID nonCompliantActorId) {
        var nonCompliantActor = this.nonCompliantActorRepository.findById(nonCompliantActorId).orElseThrow(
            nonCompliantActorNotFound(nonCompliantActorId)
        );
        return NonCompliantActorMapper.map(nonCompliantActor);
    }

    @Transactional(readOnly = true)
    public Page<NonCompliantActorDto> getNonCompliantActors(
        @Valid @NotNull NonCompliantActorFilterDto filters,
        @Valid @NotNull Pageable pageable
    ) {
        QNonCompliantActor q = QNonCompliantActor.nonCompliantActor;
        BooleanBuilder where = new BooleanBuilder();
        if (filters != null && filters.did() != null) {
            where.and(q.did.containsIgnoreCase(filters.did()));
        }
        return this.nonCompliantActorRepository.findAll(where, pageable).map(NonCompliantActorMapper::map);
    }

    @Transactional
    public NonCompliantActorDto createNonCompliantActor(
        @Valid @NotNull NonCompliantActorRequestDto request,
        String currentUserFullName
    ) {
        // Check if at least one reason in at least one language is set
        var reason = request.reason();
        var hasAnyReason = Stream.of(
            reason.reasonDe(),
            reason.reasonFr(),
            reason.reasonIt(),
            reason.reasonEn(),
            reason.reasonRm()
        ).anyMatch(value -> value != null && !value.isBlank());
        if (!hasAnyReason) {
            throw new IllegalArgumentException(
                "Validation failed: at least one reason must be provided in at least one language."
            );
        }

        // Check for already existing non-compliant actor for given did.
        // Uniqueness is enforced at the DB level (unique DID), but we also check here to provide a
        // friendly 400 error instead of a low-level constraint violation.
        if (this.nonCompliantActorRepository.existsNonCompliantActorByDid(request.did())) {
            throw new IllegalArgumentException(
                "Validation failed: non-compliant actor with given DID '" + request.did() + "' already exists."
            );
        }

        var nonCompliantActor = new NonCompliantActor(
            UUID.randomUUID(),
            request.did(),
            new NonCompliantReasonText(
                reason.reasonDe(),
                reason.reasonFr(),
                reason.reasonIt(),
                reason.reasonEn(),
                reason.reasonRm()
            )
        );
        var savedNonCompliantActor = this.nonCompliantActorRepository.save(nonCompliantActor);
        this.domainEventService.nonCompliantActorAdded(savedNonCompliantActor.getId(), currentUserFullName);
        return NonCompliantActorMapper.map(savedNonCompliantActor);
    }

    @Transactional
    public void deleteNonCompliantActor(@Valid @NotNull UUID nonCompliantActorId, String currentUserFullName) {
        this.nonCompliantActorRepository.deleteById(nonCompliantActorId);
        this.domainEventService.nonCompliantActorRemoved(nonCompliantActorId, currentUserFullName);
    }

    private static Supplier<ResourceNotFoundException> nonCompliantActorNotFound(UUID id) {
        return () -> new ResourceNotFoundException("No non-compliant actor found for id %s".formatted(id));
    }
}
