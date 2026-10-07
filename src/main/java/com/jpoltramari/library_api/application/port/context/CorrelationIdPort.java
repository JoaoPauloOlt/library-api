package com.jpoltramari.library_api.application.port.context;

import java.util.Optional;

public interface CorrelationIdPort {
    Optional<String> getCorrelationId();
}
