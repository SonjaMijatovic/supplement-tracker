# practice-todo

## BMad Method Project

This project uses the **BMad Method** for AI-assisted software development. All skills, agents, and workflows are in `_bmad/`.

### How to invoke skills

Use `/bmad-<skill-name>` or the menu codes listed by `/bmad-help`.

### Workflow phases

| Phase | Skills |
|-------|--------|
| 1 - Analysis | brainstorm, product brief, domain/market/tech research |
| 2 - Planning | PRD (required), UX design |
| 3 - Solutioning | architecture (required), epics & stories (required) |
| 4 - Implementation | sprint planning (required) → create story → dev story → code review |

### Output locations

- Planning artifacts: `_bmad-output/planning-artifacts/`
- Implementation artifacts: `_bmad-output/implementation-artifacts/`
- Project knowledge / docs: `docs/`

### Config

- User: Sonja
- Language: English
- Skill level: intermediate
- Full config: `_bmad/bmm/config.yaml`

### Installed modules

- `core` — utility skills (brainstorming, reviews, distillator, etc.)
- `bmm` — BMad Method (full product → code workflow)
- `bmb` — BMad Builder (build/edit skills and agents)
- `cis` — Creative Intelligence Suite (design thinking, storytelling, innovation)

### Context files

When starting a new session, load these if they exist:
- `docs/project-context.md` — codebase context for implementation sessions
- `_bmad-output/planning-artifacts/` — PRD, architecture, epics
- `_bmad-output/implementation-artifacts/sprint-status.yaml` — current sprint state

### Troubleshooting

When helping solve build issues, setup problems, or implementation questions, always check `CONTRIBUTING.md` first — it contains known gotchas, iOS build notes, Kotlin/CMP upgrade procedures, and architectural decisions specific to this project.
