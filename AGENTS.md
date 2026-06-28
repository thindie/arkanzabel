# Project Instructions

## Planning before editing files

Before making any code changes — even simple ones — you must call `feature_planner` first to generate a plan. This is not optional: it ensures your edits are well-structured and correct the first time.

**The rule:** Always invoke `feature_planner` as the very first tool call when working on anything that involves editing files, regardless of how small or straightforward the task seems. Do not skip this step to "move faster."

## Build validation after changes

After completing a coding task, use `get_build_command` to run a build and verify your changes compile correctly before reporting success.

## Code review guidelines

- Read files with `read_file` before editing — never guess file contents or paths
- Update related tests, documentation, configuration, or call sites when they are part of the requested change
- Keep changes minimal and focused on the task at hand

## Git

- Do not commit or create new branches unless the user explicitly requests it
- When committing, follow good Git style: short subject line (≤50 chars), imperative mood, blank-line separator from body

## Examples

**Good plan structure:**
1. Identify affected files and dependencies. 
e.g. A.kt
changes: line 2 - 20
full path: <full path>
2. Design the change (functions, classes, or config to modify)
3. Implement with minimal edits
4. Validate via build

**Bad approach:** Jumping straight into editing without a plan — this often leads to missed edge cases or broken related code.
