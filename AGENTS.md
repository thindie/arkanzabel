# Project Instructions

- code indent is 2
- if operate with UI: seek elements in unkit: toggle, loaders, buttons, screen scaffold is present.
- do not apply inlined imports, keep code clean
- Always search for existing types, utilities, and patterns in the codebase before creating new ones
- Prefer `com.thindie.engine.core` shared types (e.g., `WorkState`, `ViewState`) across features
- For implement MVI group of State, Command, Screen, stateSink - see ExmapleScreen at `com.thindie.engine.core`
- Compose Previews placed inline end of Component files
- every self-created Compose component must have the preview
- use `animate*AsState` for animations
- do not use animations inside Lazy* compose groups
- do not use LaunchedEffect for non-compose suspend calls

## commiting
- check git diff, git status.
- determine the atomic logical groups of changes, sepate them into commits
- the pattern (non conventional):
```
[<feature context>] Commit message

Commit body
```

## Restricted code constructs

- runblocking
- try { } catch Exception in suspend context, return Result<X> in suspend context
- mutable variables

## Build validation after changes

After completing a coding task, use `get_build_command` to run a build and verify your changes compile correctly before reporting success.
Use ktLintFormat also.






