package org.specter.converter.aplication.inport;

import lombok.AllArgsConstructor;
import org.specter.converter.domain.ConverterCore;

@AllArgsConstructor
public class ConverterInPortImpl implements ConverterInPort{
  private final ConverterCore core;

  @Override
  public boolean checkAvailableStr(String message) {
    return core.checkAvailableStr(message);
  }

  @Override
  public String engToKor(String message) {
    return core.engToKor(message);
  }
}
