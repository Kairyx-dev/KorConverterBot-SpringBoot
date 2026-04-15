package org.specter.converter.adapter.jpa.configuration;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories("org.specter.converter.adapter.jpa")
@EntityScan(basePackages = "org.specter.converter.adapter.jpa")
public class JpaConfiguration {

}
