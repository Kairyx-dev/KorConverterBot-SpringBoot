package org.specter.converter.boot.configuration;

import org.specter.converter.aplication.inport.DiscordBotInPort;
import org.specter.converter.aplication.inport.DiscordBotInPortImpl;
import org.specter.converter.aplication.outport.JpaOutPort;
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
  public DiscordBotInPort getConverterInPort(ConverterCore core, JpaOutPort jpaOutPort) {
    return new DiscordBotInPortImpl(core, jpaOutPort);
  }
}
