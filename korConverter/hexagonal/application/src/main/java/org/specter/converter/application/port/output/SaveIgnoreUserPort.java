package org.specter.converter.application.port.output;

import org.specter.converter.domain.model.IgnoreUser;

public interface SaveIgnoreUserPort {
  void save(IgnoreUser ignoreUser);

  void delete(IgnoreUser ignoreUser);
}
