package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.enums.UserStatus;

public interface UserSecuritySnapshotView {

    Long getId();

    Integer getTokenVersion();

    UserStatus getStatus();
}
