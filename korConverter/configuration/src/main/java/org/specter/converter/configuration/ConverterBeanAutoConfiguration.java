package org.specter.converter.configuration;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.lang.reflect.Proxy;
import java.time.Clock;
import org.specter.converter.adapter.persistence.port.IgnoreUserPersistenceAdapter;
import org.specter.converter.adapter.persistence.port.IgnoreUserQueryAdapter;
import org.specter.converter.adapter.persistence.port.MessageLogRecordAdapter;
import org.specter.converter.application.port.input.AddIgnoreUserUseCase;
import org.specter.converter.application.port.input.CheckIgnoreUserUseCase;
import org.specter.converter.application.port.input.ConvertMessageUseCase;
import org.specter.converter.application.port.input.RemoveIgnoreUserUseCase;
import org.specter.converter.application.service.AddIgnoreUserService;
import org.specter.converter.application.service.CheckIgnoreUserService;
import org.specter.converter.application.service.ConvertMessageService;
import org.specter.converter.application.service.RemoveIgnoreUserService;
import org.specter.converter.domain.service.ConversionDomainService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ReflectionUtils;

@AutoConfiguration
public class ConverterBeanAutoConfiguration {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  public ConversionDomainService conversionDomainService() {
    return new ConversionDomainService();
  }

  @Bean
  public AddIgnoreUserUseCase addIgnoreUserUseCase(
      IgnoreUserPersistenceAdapter adapter,
      Clock clock,
      PlatformTransactionManager txManager,
      ObservationRegistry registry) {
    return observed(
        createTxProxy(
            new AddIgnoreUserService(adapter, adapter, clock),
            AddIgnoreUserUseCase.class,
            txManager),
        "converter.ignore-user.add",
        registry);
  }

  @Bean
  public RemoveIgnoreUserUseCase removeIgnoreUserUseCase(
      IgnoreUserPersistenceAdapter adapter,
      Clock clock,
      PlatformTransactionManager txManager,
      ObservationRegistry registry) {
    return observed(
        createTxProxy(
            new RemoveIgnoreUserService(adapter, adapter, clock),
            RemoveIgnoreUserUseCase.class,
            txManager),
        "converter.ignore-user.remove",
        registry);
  }

  @Bean
  public ConvertMessageUseCase convertMessageUseCase(
      ConversionDomainService conversionService,
      MessageLogRecordAdapter messageLogAdapter,
      PlatformTransactionManager txManager,
      ObservationRegistry registry) {
    return observed(
        createTxProxy(
            new ConvertMessageService(conversionService, messageLogAdapter),
            ConvertMessageUseCase.class,
            txManager),
        "converter.message.convert",
        registry);
  }

  @Bean
  public CheckIgnoreUserUseCase checkIgnoreUserUseCase(
      IgnoreUserQueryAdapter queryAdapter,
      PlatformTransactionManager txManager,
      ObservationRegistry registry) {
    return observed(
        createReadOnlyTxProxy(
            new CheckIgnoreUserService(queryAdapter), CheckIgnoreUserUseCase.class, txManager),
        "converter.ignore-user.check",
        registry);
  }

  @SuppressWarnings("unchecked")
  private <T> T createTxProxy(T target, Class<T> iface, PlatformTransactionManager txManager) {
    var template = new TransactionTemplate(txManager);
    return (T)
        Proxy.newProxyInstance(
            iface.getClassLoader(),
            new Class<?>[] {iface},
            (proxy, method, args) ->
                template.execute(status -> ReflectionUtils.invokeMethod(method, target, args)));
  }

  @SuppressWarnings("unchecked")
  private <T> T createReadOnlyTxProxy(
      T target, Class<T> iface, PlatformTransactionManager txManager) {
    var template = new TransactionTemplate(txManager);
    template.setReadOnly(true);
    return (T)
        Proxy.newProxyInstance(
            iface.getClassLoader(),
            new Class<?>[] {iface},
            (proxy, method, args) ->
                template.execute(status -> ReflectionUtils.invokeMethod(method, target, args)));
  }

  // Observation proxy wraps the outermost layer (outside TX proxy) so that
  // the metric captures total UseCase execution including transaction overhead.
  // Micrometer/ObservationRegistry are used only here in Configuration module,
  // never in Domain (D-1) or Application (A-1) — per Part 10 §10.1.
  @SuppressWarnings("unchecked")
  private <T> T observed(T target, String name, ObservationRegistry registry) {
    return (T)
        Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            target.getClass().getInterfaces(),
            (proxy, method, args) ->
                Observation.createNotStarted(name, registry)
                    .observe(() -> ReflectionUtils.invokeMethod(method, target, args)));
  }
}
