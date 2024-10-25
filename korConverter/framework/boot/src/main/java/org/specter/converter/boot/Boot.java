package org.specter.converter.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    scanBasePackages = "org.specter.converter"
)
public class Boot {

  public static void main(String[] args) {
    SpringApplication.run(Boot.class, args);
  }
}