package com.jpoltramari.library_api.infrastructure.web;

import com.jpoltramari.library_api.application.port.context.CorrelationIdPort;
import com.jpoltramari.library_api.infrastructure.security.filter.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MdcCorrelationIdAdapter implements CorrelationIdPort {

    @Override
    public Optional<String> getCorrelationId() {
        return Optional.ofNullable(MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}
