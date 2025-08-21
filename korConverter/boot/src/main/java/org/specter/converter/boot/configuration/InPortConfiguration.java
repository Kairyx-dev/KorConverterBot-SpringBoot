package org.specter.converter.boot.configuration;

import org.specter.converter.aplication.inport.DiscordBotInPort;
import org.specter.converter.aplication.inport.DiscordBotInPortImpl;
import org.specter.converter.aplication.outport.JpaOutPort;
import org.specter.converter.domain.core.ConverterCoreV2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InPortConfiguration {

  @Bean
  public ConverterCoreV2 getConverterCore() {
    return new ConverterCoreV2();
  }

  @Bean
  public DiscordBotInPort getConverterInPort(ConverterCoreV2 core, JpaOutPort jpaOutPort) {
    return new DiscordBotInPortImpl(core, jpaOutPort);
  }
}
