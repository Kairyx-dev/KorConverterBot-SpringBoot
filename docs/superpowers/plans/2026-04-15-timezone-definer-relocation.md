# TimeZoneDefiner Relocation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore the Logback `PropertyDefiner` (lost in commit `2f09b53` when the `common` module was removed) so `${TIMEZONE}` resolves to the JVM's default timezone ID in log output.

**Architecture:** Place the class in the `configuration` module (`org.specter.converter.configuration.logging.TimeZoneDefiner`) per purist-ddd-playbook Part 10 §10.1 (logback custom extensions) and Part 1 §7.2 (cross-cutting infrastructure). No new dependency declarations — `logback-core` is already on `:configuration`'s compileClasspath via `spring-boot-starter-logging`. The only other change is updating the FQCN reference in `runtime/cfg/logback-spring.xml`.

**Tech Stack:** Java 25, Logback 1.5.32 (via Spring Boot 4.0.x starter), Gradle 9.x, Spotless (google-java-format 1.35.0), Checkstyle.

**Design spec:** `docs/superpowers/specs/2026-04-15-timezone-definer-relocation-design.md`

**No tests:** Per spec §3 (비목표). 10-line JDK pass-through (`TimeZone.getDefault().getID()`); unit testing would re-test the JDK. Runtime verification happens at step 1.9 of Task 1.

---

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `korConverter/configuration/src/main/java/org/specter/converter/configuration/logging/TimeZoneDefiner.java` | Create | Logback `PropertyDefiner` SPI implementation returning JVM default timezone ID |
| `runtime/cfg/logback-spring.xml` | Modify (line 3) | Update FQCN from the deleted `common.utils.TimeZoneDefiner` to the new `configuration.logging.TimeZoneDefiner` |

No build-script changes. No module layout changes.

---

## Environment Note for Executor

`./gradlew` requires **JVM 17+** but the default shell PATH may point to JDK 8 (`~/.sdkman/candidates/java/current` → 8.0.472). Before running any Gradle command, export Java 25:

```bash
export JAVA_HOME=~/.sdkman/candidates/java/25.0.2-amzn
export PATH=$JAVA_HOME/bin:$PATH
java -version   # expect: openjdk version "25" ...
```

Keep this environment for the whole session.

---

## Task 1: Relocate TimeZoneDefiner and update logback FQCN

**Files:**
- Create: `korConverter/configuration/src/main/java/org/specter/converter/configuration/logging/TimeZoneDefiner.java`
- Modify: `runtime/cfg/logback-spring.xml` (line 3, `<define class="...">` attribute only)

- [ ] **Step 1: Create the package directory**

Run:
```bash
mkdir -p korConverter/configuration/src/main/java/org/specter/converter/configuration/logging
```

Expected: no output. The directory tree `korConverter/configuration/src/main/java/org/specter/converter/configuration/` already exists (contains `ConverterBeanAutoConfiguration.java`); only the leaf `logging/` segment is new.

- [ ] **Step 2: Write `TimeZoneDefiner.java`**

Create `korConverter/configuration/src/main/java/org/specter/converter/configuration/logging/TimeZoneDefiner.java` with exactly:

```java
package org.specter.converter.configuration.logging;

import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.PropertyDefiner;
import java.util.TimeZone;

public class TimeZoneDefiner extends ContextAwareBase implements PropertyDefiner {

  @Override
  public String getPropertyValue() {
    return TimeZone.getDefault().getID();
  }
}
```

Notes:
- 2-space indent (google-java-format default used by Spotless in this repo).
- Class must be `public` and non-`final`: Logback instantiates it via reflection (`Class.forName(...).getConstructor().newInstance()`), and `ContextAwareBase` is not final.
- Do **not** add JSpecify `@Nullable`/`@NonNull` annotations. Rule AD-6 permits them in Configuration, but they add zero value here (no nullable members, no nullable return — `TimeZone.getDefault()` is never null).

- [ ] **Step 3: Update `runtime/cfg/logback-spring.xml` line 3**

Before:
```xml
<define class="org.specter.converter.common.utils.TimeZoneDefiner" name="TIMEZONE"/>
```

After:
```xml
<define class="org.specter.converter.configuration.logging.TimeZoneDefiner" name="TIMEZONE"/>
```

Only the `class` attribute changes. The element's position (line 3), the `name="TIMEZONE"` attribute, indentation, and self-closing form all stay identical.

- [ ] **Step 4: Verify formatting with Spotless apply**

Run:
```bash
./gradlew :configuration:spotlessApply
```

Expected: `BUILD SUCCESSFUL`. If the google-java-format reformatter rewrites the file, the changes should be limited to whitespace — no semantic drift.

- [ ] **Step 5: Compile the configuration module**

Run:
```bash
./gradlew :configuration:compileJava
```

Expected: `BUILD SUCCESSFUL`. This proves that `ch.qos.logback.core.spi.PropertyDefiner` and `ContextAwareBase` resolve on the compileClasspath (via the transitive `spring-boot-starter-logging` → `logback-classic` → `logback-core` path confirmed during brainstorming).

If this step fails with "package ch.qos.logback.core.spi does not exist", the transitive path has changed. Stop and add `compileOnly(libs.logback.core)` to `korConverter/configuration/build.gradle.kts` (see design spec §4.5), re-run, and note the dependency change in the commit message.

- [ ] **Step 6: Run the full build**

Run:
```bash
./gradlew build
```

Expected: `BUILD SUCCESSFUL`. This also exercises ArchUnit, Spotless check, Checkstyle, tests, and JaCoCo — catching any rule drift (e.g. an ArchUnit rule forbidding `ch.qos.logback.*` imports in configuration).

If ArchUnit flags the new logback import in configuration, re-read `.claude/rules/validation.md` — no such restriction exists today, but a newly added rule would surface here. In that case, stop and consult the user; do not disable the rule unilaterally.

- [ ] **Step 7: Checkstyle + Spotless explicit verification**

Run:
```bash
./gradlew spotlessCheck checkstyleMain
```

Expected: `BUILD SUCCESSFUL`. (Also implicitly covered by step 6, but run explicitly to surface style issues with a clean signal.)

- [ ] **Step 8: Verify the FQCN reference reads correctly**

Run:
```bash
grep -n "TimeZoneDefiner" runtime/cfg/logback-spring.xml
```

Expected output (exactly):
```
3:  <define class="org.specter.converter.configuration.logging.TimeZoneDefiner" name="TIMEZONE"/>
```

- [ ] **Step 9: Runtime smoke check (optional, requires env)**

Only run if the executor has `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER_NAME`, `DB_PASSWORD`, and `DISCORD_BOT_TOKEN` set in the environment. Otherwise skip and note "runtime verification deferred" in the PR/commit description.

Run:
```bash
./gradlew bootRun
```

Expected behaviour: within ~5 seconds of startup, log lines to stdout should match the pattern defined in `runtime/cfg/logback-spring.xml:7`:
```
<ISO8601 timestamp> <TIMEZONE-ID> INFO  --- [ ... ] ...
```
where `<TIMEZONE-ID>` is the JVM default (e.g. `Asia/Seoul`, `UTC`), **not** the literal string `UNDEFINED`. Kill the process (`Ctrl-C`) once verified.

- [ ] **Step 10: Commit**

Run:
```bash
git status
git add korConverter/configuration/src/main/java/org/specter/converter/configuration/logging/TimeZoneDefiner.java runtime/cfg/logback-spring.xml
git status
```

Expected second `git status`: both files listed under "Changes to be committed" — and nothing else. If the working tree has unexpected changes (e.g., from step 4's spotlessApply reformatting other files), stop and investigate before committing.

Then commit:
```bash
git commit -m "$(cat <<'EOF'
feat(configuration): restore TimeZoneDefiner as logback PropertyDefiner

Re-adds the custom logback PropertyDefiner that was removed together with
the common module in 2f09b53 during purist DDD migration. New location is
the configuration module per purist-ddd-playbook Part 10 §10.1 (logback
custom extensions) and Part 1 §7.2 (cross-cutting infrastructure).

Also updates runtime/cfg/logback-spring.xml to point at the new FQCN so
the ${TIMEZONE} pattern variable resolves to the JVM default timezone
ID instead of the "UNDEFINED" fallback.

No dependency changes: logback-core is already on the configuration
module's compileClasspath via spring-boot-starter-logging.

See docs/superpowers/specs/2026-04-15-timezone-definer-relocation-design.md

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
EOF
)"
```

Expected: `[feature/purist-ddd-migration <sha>] feat(configuration): restore TimeZoneDefiner as logback PropertyDefiner` with `2 files changed, 11 insertions(+), 1 deletion(-)` (±1 depending on Spotless reflow).

- [ ] **Step 11: Update the design spec status and ADR index (if applicable)**

Check if the design spec's "상태" field should change from `Approved` to `Implemented`.

Run:
```bash
grep -n "상태" docs/superpowers/specs/2026-04-15-timezone-definer-relocation-design.md
```

If desired, update `Approved` → `Implemented (<short-sha>)` and commit in a separate commit:
```bash
git add docs/superpowers/specs/2026-04-15-timezone-definer-relocation-design.md
git commit -m "docs: mark TimeZoneDefiner relocation spec as implemented

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

Skip this step if the team workflow keeps spec status churn out of the PR.

---

## Self-Review (completed at plan write time)

**Spec coverage:**
| Spec section | Covered by |
|--------------|-----------|
| §4.1 Module placement | Step 1–2 |
| §4.3 Class body | Step 2 |
| §4.4 XML FQCN update | Step 3 |
| §4.5 No dependency changes | Step 5 (guarded with fallback to `compileOnly(libs.logback.core)` if transitive path is broken) |
| §6.1–§6.4 Build / Spotless / Checkstyle verification | Steps 5, 6, 7 |
| §6.5 Runtime log output check | Step 9 |
| §6.6 `validation.md` import rules | Step 6 (ArchUnit inside `./gradlew build`) |
| §9 Exit criteria | Steps 2, 3, 6, 9 |

**Placeholder scan:** no TBD/TODO; every step has exact commands, exact file content, and exact expected output.

**Type consistency:** single class, no cross-task type references. Package path (`org.specter.converter.configuration.logging`) and class name (`TimeZoneDefiner`) match between Step 2 (source) and Step 3 (XML FQCN).

No gaps found.
