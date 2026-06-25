package ch.admin.bj.swiyu.trust.management.modules.common.exception;

import lombok.*;
import org.springframework.web.client.*;

@RequiredArgsConstructor
public class ExternalSystemProxyException extends RuntimeException {

    @Getter
    private final HttpClientErrorException httpClientErrorException;
}
