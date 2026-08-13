package ch.admin.bj.swiyu.trust.management.modules.management.service;

import java.util.List;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@UtilityClass
public class ProtectedVerificationAuthorizationMapper {

    private final List<String> allowedSortFields = List.of("updatedAt");

    public static Pageable mapPageableWithValidSortProperties(Pageable pageable) {
        // filter Pageable for invalid sort fields to not fail them silently
        pageable
            .getSort()
            .stream()
            .filter(c -> !allowedSortFields.contains(c.getProperty()))
            .findFirst()
            .ifPresent(c -> {
                throw new IllegalArgumentException("Invalid pagination parameters");
            });

        // Map Pageable fields to actual DB entities
        var sort = pageable
            .getSort()
            .stream()
            .map(order -> {
                String property = order.getProperty();
                if ("updatedAt".equals(property)) {
                    return new Sort.Order(order.getDirection(), "audit.lastModifiedAt");
                }
                return order;
            })
            .toList();
        if (pageable.isUnpaged()) {
            return Pageable.unpaged(Sort.by(sort));
        }
        // Copy Pageable details over as a Pageable is immutable
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(sort));
    }
}
