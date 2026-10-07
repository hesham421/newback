# ADR-SEC-034 — The VIEW gateway is part of the structural path to an action, so the consumer's role is module + screen + VIEW + CREATE
Status      : ACCEPTED
Stage       : P1        Module: SEC        Version: v2
Lane        : operator (human decision on the breaking ambiguity of ADR-SEC-031)
Decided     : 2026-09-23
traces      : US-SEC-014, POL-SEC-012, POL-SEC-013, POL-SEC-001, POL-SEC-002, REQ-SEC-030, REQ-SEC-044, RULE-SEC-007, AC-SEC-077

## Decision
The Oracle event consumer's role holds the FIN module grant, the journal-entry screen grant, that
screen's **VIEW** action grant and its **create-journal-entry** action grant, and nothing else.

"Exactly one permission", as the change request uses the phrase, counts the one **business capability**
the consumer is given — create a journal entry in FIN. The module grant, the screen grant and VIEW are
the structural path to that capability, in the same sense POL-SEC-001 and POL-SEC-002 already make the
module and screen grants structural. They are not additional capabilities.

This supersedes ADR-SEC-031 (BLOCKED) and overrides the "no VIEW grant" clause of ADR-SEC-018 and of
business-policies-sec.md RESOLVED DECISIONS #4. Those two remain as written — a committed ADR and a
committed decision row are not edited — and this ADR is the later word on that one clause.

## Why
The alternative readings each cost more than they are worth:

- Exempting SERVICE principals from the gateway (option B of ADR-SEC-031) reopens REQ-SEC-030 and
  RULE-SEC-007, which SEC's own P2 and P3.1, the test plans and FIN v2's access summary all rely on. The
  change request's acceptance 2 says v1 invariants are inherited, not reopened. Making the change set
  BREAKING to relax a security rule is the wrong trade for a wording problem.
- Changing the platform's `conventions.security_model.gateway_action` (option C) is not this module's to
  make and would reach every module.
- A dedicated FIN event-intake page whose VIEW exposes no read endpoint (option D) is the only option
  under which "nothing else" stays literally true, and it was weighed and not taken: it needs a new FIN
  change set, and FIN v2 has already passed gate 1.

## Consequence — stated plainly, because it is a real widening
The consumer can **read and search FIN journal entries**, not only create them. That is what VIEW on the
journal-entry screen carries in v1 as built. It cannot reach any other FIN screen and no other module,
because the module gate and the screen grant still bind (POL-SEC-001, POL-SEC-002).

If least privilege on FIN reads later matters for this integration — a daemon that can enumerate the
ledger is a wider blast radius than one that can only append to it — option D is the route, as its own
FIN change set. Nothing here forecloses it.

- AC-SEC-077 under REQ-SEC-044 states the role shape and asserts both halves: the role saves without
  SEC-409-NO-VIEW-GRANT, and every other screen and module is denied.
- No v1 REQ, RULE, DBF, API or FIN artifact changes. The change set stays ADDITIVE.

Source : ADR-SEC-031 options table, option A; human decision at the P1 breaking-ambiguity stop
