# Recommended Implementation Order

1. Finish Task 1 first because empty conflict targets and missing strategy fields drive the current upsert failures.
2. Finish Task 2 next because match-field upsert depends on transactional `beforeExecute` preparation and `afterExecute` rollback behavior.
3. Finish Task 3 after implementation so verification evidence reflects the final branch state.
