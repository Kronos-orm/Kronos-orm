# Shared logging switch and adapter selection

## Observed behavior

Issue #292 reports that `KLoggerType.NONE` did not stop output. It was also used as the
sentinel for an explicitly assigned factory, so treating it as disabled would break custom
logging. The JVM bundled logger queued messages before checking an empty destination list,
which could replay discarded messages after a destination was restored.

## Verified correction

`Kronos.logging` owns `enabled`, `path`, `type`, `minimumLevel` and optional `factory`.
The defaults are enabled output, the `console` destination and the bundled logger. There
is no NONE adapter. A custom factory is independent of the selected type, and platform
detection leaves it intact. Internal calls use `logging.logger(source)` so custom loggers
receive the same global switch and minimum-level filtering as bundled/platform loggers.

The shared wrapper checks configuration before creating an adapter and before lazy message
construction. Retained wrappers observe later switch, level and factory changes. Adapter
checks still apply, and disabling/re-enabling preserves configuration. Destination filtering
belongs to bundled adapters; it must not disable a custom or framework adapter.

Check disabled output and empty destinations before enqueueing JVM bundled messages. Discard
pending tasks when output is disabled rather than replaying them later. Forward exceptions
to adapters and retain stack traces in JVM bundled and Native console output.

## Verification

JVM tests passed for core (1,019), logging (6), and codegen (129). Shared configuration tests
cover every level, lazy messages, factory creation, retained loggers, backend filtering,
custom-factory precedence and exception forwarding. JVM tests verify discarded messages do
not reappear, exceptions reach files, and disabled SQL logging never formats parameters.
Native adapter tests are located in `kronos-logging/test@native`, covering output suppression,
restoration and exception details on every Native runtime in CI. Their runtime validation
requires a supported Native host SDK.
