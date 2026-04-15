package org.specter.converter.configuration;

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
import org.specter.converter.domain.model.ConversionDomainService;
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
      IgnoreUserPersistenceAdapter adapter, Clock clock, PlatformTransactionManager txManager) {
    return createTxProxy(
        new AddIgnoreUserService(adapter, adapter, clock), AddIgnoreUserUseCase.class, txManager);
  }

  @Bean
  public RemoveIgnoreUserUseCase removeIgnoreUserUseCase(
      IgnoreUserPersistenceAdapter adapter, Clock clock, PlatformTransactionManager txManager) {
    return createTxProxy(
        new RemoveIgnoreUserService(adapter, adapter, clock),
        RemoveIgnoreUserUseCase.class,
        txManager);
  }

  @Bean
  public ConvertMessageUseCase convertMessageUseCase(
      ConversionDomainService conversionService,
      MessageLogRecordAdapter messageLogAdapter,
      PlatformTransactionManager txManager) {
    return createTxProxy(
        new ConvertMessageService(conversionService, messageLogAdapter),
        ConvertMessageUseCase.class,
        txManager);
  }

  @Bean
  public CheckIgnoreUserUseCase checkIgnoreUserUseCase(
      IgnoreUserQueryAdapter queryAdapter, PlatformTransactionManager txManager) {
    return createReadOnlyTxProxy(
        new CheckIgnoreUserService(queryAdapter), CheckIgnoreUserUseCase.class, txManager);
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
}
