package org.specter.converter.application.port.input;

import org.specter.converter.application.dto.query.CheckIgnoreUserQuery;

public interface CheckIgnoreUserUseCase {
    boolean execute(CheckIgnoreUserQuery query);
}
