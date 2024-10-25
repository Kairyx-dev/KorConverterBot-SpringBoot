package org.specter.converter.boot.configuration;

import org.specter.converter.aplication.inport.ConverterInPort;
import org.specter.converter.aplication.inport.ConverterInPortImpl;
import org.specter.converter.domain.ConverterCore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InPortConfiguration {

  @Bean
  public ConverterCore getConverterCore() {
    return new ConverterCore();
  }

  @Bean
  public ConverterInPort getConverterInPort(ConverterCore core) {
    return new ConverterInPortImpl(core);
  }
}
