package org.specter.converter.common.utils;

import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.PropertyDefiner;
import java.util.TimeZone;

public class TimeZoneDefiner extends ContextAwareBase implements PropertyDefiner {

  @Override
  public String getPropertyValue() {
    return TimeZone.getDefault().getID();
  }
}
