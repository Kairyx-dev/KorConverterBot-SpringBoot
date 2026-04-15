# Aggregate / UseCase Scaffold Templates

> 새 Aggregate 또는 UseCase 추가 시 이 템플릿을 참조한다.
> Inside-Out 순서: Domain → Application → Adapter → Configuration → DDL

## 신규 Aggregate 추가 체크리스트

| # | 모듈 | 파일 | 근거 |
|---|------|------|------|
| 1 | domain | `{Subject}Id.java` (VO) | D-6, D-7 |
| 2 | domain | 기타 VO들 | D-6 |
| 3 | domain | `{Subject}Status.java` (enum) | — |
| 4 | domain | `{Subject}Event.java` (sealed interface) | D-13 |
| 5 | domain | `{Subject}CreatedEvent.java` 등 (record) | D-13 |
| 6 | domain | `{Subject}Exception.java` (sealed class) | D-13 |
| 7 | domain | 하위 Exception들 (final class) | D-13 |
| 8 | domain | `{Subject}.java` (Aggregate Root) | D-5, D-8, D-14 |
| 9 | application | `Create{Subject}UseCase.java` (interface) | A-2 |
| 10 | application | `Create{Subject}Command.java` (record, 원시 타입) | A-7, A-10 |
| 11 | application | `{Subject}Result.java` (record, 원시 타입) | A-10 |
| 12 | application | `Load{Subject}Port.java` | A-3 |
| 13 | application | `Save{Subject}Port.java` | A-3, AD-3, AD-7 |
| 14 | application | `{Subject}QueryPort.java` | A-3 |
| 15 | application | `Create{Subject}Service.java` | A-1, A-5, A-6 |
| 16 | adapter-out | `{Subject}PersistenceMapper.java` | AD-4, AD-5 |
| 17 | adapter-out | `{Subject}PersistenceAdapter.java` (implements Load+Save) | AD-3, AD-7 |
| 18 | adapter-out | `{Subject}QueryAdapter.java` (implements QueryPort) | A-3 |
| 19 | adapter-in | Listener 또는 Controller에 UseCase 연결 | AD-1 |
| 20 | configuration | BeanConfiguration Bean + TX 프록시 | A-4, T-1 |
| 21 | resources | DDL migration | — |
| 22 | — | 테스트 | 70% Domain / 15% App / 10% Adapter / 5% E2E |

## 신규 UseCase 추가 시

```
[Application]
dto/command/{Verb}{Subject}Command.java
port/input/{Verb}{Subject}UseCase.java
service/{Verb}{Subject}Service.java

[Adapter — 기존 Listener/Controller에 엔드포인트 추가]

[Configuration — BeanConfiguration에 Bean + TX 프록시 추가]

[Domain — 필요 시]
{Subject}.java ← 행위 메서드 추가
{Subject}{PastTenseVerb}Event.java ← 이벤트 추가
{Subject}Event.java ← permits 추가
```

## TX 프록시 패턴 (Configuration 모듈)

```java
@Configuration
public class {Context}BeanConfiguration {

    @Bean
    public {Verb}{Subject}UseCase {verb}{Subject}UseCase(
            {Subject}PersistenceAdapter adapter, Clock clock,
            PlatformTransactionManager txManager) {
        return createTxProxy(
            new {Verb}{Subject}Service(adapter, clock),
            {Verb}{Subject}UseCase.class,
            txManager
        );
    }

    @SuppressWarnings("unchecked")
    private <T> T createTxProxy(T target, Class<T> iface,
                                 PlatformTransactionManager txManager) {
        var template = new TransactionTemplate(txManager);
        return (T) Proxy.newProxyInstance(
            iface.getClassLoader(),
            new Class<?>[]{iface},
            (proxy, method, args) -> template.execute(status ->
                ReflectionUtils.invokeMethod(method, target, args)
            )
        );
    }
}
```

---

> 위 규칙을 현재 상황에 적용하기 어렵거나 규칙 간 충돌이 발생하면,
> 명시된 ADR 번호(ADR-NNNN)에 해당하는 docs/decisions/ 파일을 직접 읽어
> 결정의 배경을 파악한 후 최적의 대안을 제안하라.
