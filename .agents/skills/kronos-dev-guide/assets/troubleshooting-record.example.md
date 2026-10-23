# Example: stale generated test fixture

This illustrative example shows the record format; it is not a Kronos failure report. Copy to your local `evolution/` directory and replace it with verified evidence.

## Symptom

A test reports an expected-output mismatch after its input fixture changes.

## Confirmed cause

The generated expected output was not refreshed with the input.

## Resolution and verification

Regenerate the affected fixture, inspect the diff, and run the targeted test. Record the actual command and result here.

## Prevention

Update fixture inputs and their generated expectations together.
