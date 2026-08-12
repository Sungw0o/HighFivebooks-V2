# Testing

## Local test run

Run the default test suite without coverage instrumentation:

```bash
./mvnw test
```

On Windows:

```powershell
.\mvnw.cmd test
```

If `JAVA_HOME` is not configured on Windows, point it at a local JDK before running the wrapper.

JaCoCo is kept behind the `coverage` Maven profile because its Java agent can fail on local Windows environments with non-ASCII user paths. CI still runs with coverage enabled.

## Coverage run

Use the coverage profile in CI or in local environments with an ASCII-safe Maven/JDK path:

```bash
./mvnw test -Pcoverage
```

## Test isolation

Tests use `src/test/resources/application.yml` to disable Spring Cloud Config and Eureka. Unit and slice tests should not require external infrastructure just to boot an application context.

## RabbitMQ DLQ integration

`RabbitMqDlqIntegrationTest` declares the production queue/DLX/DLQ topology on a real broker, rejects a source message without requeue, and verifies the payload and `x-death` header in the DLQ. It is skipped unless explicitly enabled.

```powershell
$env:RUN_RABBITMQ_INTEGRATION='true'
$env:RABBITMQ_HOST='localhost'
$env:RABBITMQ_PORT='5672'
$env:RABBITMQ_USERNAME='guest'
$env:RABBITMQ_PASSWORD='guest'
.\mvnw.cmd '-Dtest=RabbitMqDlqIntegrationTest' test
```

Unset the variables after the run if they should not remain in the shell session.
