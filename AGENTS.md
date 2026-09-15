# Working agreement

- Default working branch: `Hung`. Before editing, check `git branch --show-current`.
- Do not work directly on `main` or `develop`. If on either, switch to `Hung` without discarding local changes.
- Integration flow: personal branch -> pull request into `develop` -> pull request into `main`.
- Push or merge only when the user requests it. Never force-push shared branches.
- Write focused commits with a clear purpose and describe affected behavior and verification.
- Run `npm run build:web` for web/shared TypeScript changes. Run Maven tests or the backend Docker build for Java changes.
- Never commit local environment files, credentials, database dumps, dependencies or runtime logs.
