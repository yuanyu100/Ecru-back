---
name: thesis-reference-grounding
description: Use when the user is writing or revising an undergraduate thesis and wants real references, literature leads, or grounded related-work text instead of fabricated citations. Enforces that references must come from user-provided source files, verified searches, or explicit bibliographic evidence, and helps reuse trustworthy material from proposal or opening-report documents without importing unsupported implementation claims.
---

# Thesis Reference Grounding

Use this skill when the user asks to:
- find or rebuild thesis references
- reuse literature or related-work text from an opening report
- check whether citations are real
- replace fabricated bibliography entries
- draft introduction or research-status sections from trustworthy source material

## Core rules

- Never invent references, DOIs, page ranges, authors, journals, or publication years.
- Never keep a citation in the thesis unless at least one concrete source artifact exists:
  - a user-provided DOCX/PDF/Markdown file
  - a verified bibliographic record from a trusted source
  - a clearly attributable official page
- Distinguish three states explicitly:
  - `verified`: bibliographic information is supported by a concrete source
  - `lead only`: mentioned in source material but not yet independently checked
  - `remove`: likely fabricated, incomplete, or irrelevant to the real project
- If the thesis scope and the source scope differ, reuse only background framing and reference leads. Do not import unsupported implementation claims.

## Grounding workflow

1. Identify the working thesis draft and the user-provided evidence files.
2. Extract candidate references from opening reports, proposal documents, older drafts, or teacher-provided notes.
3. Split extracted material into:
   - reusable background wording
   - reusable reference leads
   - unsupported project claims to discard
4. For each candidate reference, keep a compact evidence log:
   - where it was found
   - whether it is verified or only a lead
   - whether it matches the actual thesis topic
5. Only after grounding the references, rewrite related-work or introduction text around the real project scope.

## Opening-report reuse rules

- Opening reports often contain real literature but broader or drifting technical claims.
- Safe to reuse:
  - general background on the domain
  - problem significance
  - real reference leads
  - high-level research-trend framing after scope adjustment
- Unsafe to reuse without proof in the repository:
  - tech stacks not used by the project
  - modules that were never implemented
  - performance conclusions
  - broad comparative claims not supported by evidence

## Writing rules for thesis integration

- Rewrite rather than paste long source paragraphs.
- Keep the thesis centered on the actual repository and implemented system.
- If the user says references are not the current priority, keep placeholders rather than fake entries.
- When a related-work section cannot yet be safely completed, say so and leave a traceable placeholder.

## Expected outputs

Depending on the request, produce one or more of:
- a cleaned bibliography candidate list
- a “verified / lead only / remove” triage table
- a safer introduction or related-work rewrite
- a note describing which opening-report content was reused and which was rejected
