# Agent instructions

Before modifying this repository, read and follow [.agents/skills/personal-gemini-journal/SKILL.md](.agents/skills/personal-gemini-journal/SKILL.md). It is the machine-readable project contract for architecture, security, Google Maps handling, testing, and focused commits.

User identity must always come from the verified backend principal. Never add client-supplied UID ownership, frontend secrets, direct browser database writes, or unscoped persistence queries.
