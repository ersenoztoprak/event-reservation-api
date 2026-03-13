package com.ing.assesment.domain.auth.port;

import com.ing.assesment.domain.auth.model.AuthenticatedUser;

public interface CurrentUserPort {
    AuthenticatedUser getCurrentUser();
}
