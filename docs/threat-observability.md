# Threat observability prototype

A small "threat observability" layer on top of the ledger API. It watches the
account API, detects suspicious behaviour, enriches and scores it, and folds
the noise into a short list of findings an analyst can act on.

It is a learning prototype: the point is to show the *shape* of such a system
(sensor -> events -> detections -> enrichment -> scoring -> findings), not to be
a production SIEM.

## Pipeline

```
HTTP request
   |
   v
SecurityEventInterceptor   (sensor: builds a SecurityEvent after each /api/accounts/** call)
   |
   v
DetectionEngine            (buffers the event in EventWindow, runs every DetectionRule)
   |   Detection = one rule hit, mapped to OWASP + MITRE ATT&CK
   v
FindingService             (enrich + score + deduplicate)
   |   - ThreatIntelProvider: is the source IP known-bad?
   |   - account balance: how valuable is the target?
   |   - fingerprint: same rule + actor + IP => same finding, occurrenceCount++
   v
Finding (security_finding table)  ->  /api/security/*  ->  Angular "Security" page
```

Code lives in `ledger-api/src/main/java/com/gattopiccolo/ledger/threat`.
Everything except the thin Spring/JPA wiring is plain Java with no framework
dependencies, which keeps the logic easy to unit-test.

## Detection rules

| Rule | What it detects | OWASP Top 10:2025 | MITRE ATT&CK |
|---|---|---|---|
| LEDGER-001 | A caller touches an account owned by someone else (BOLA/IDOR). Critical if a state-changing call succeeded, high if a read succeeded, medium if rejected. | A01 Broken Access Control | T1190 Exploit Public-Facing Application (Initial Access) |
| LEDGER-002 | One actor probes many foreign or non-existent account ids in a window (enumeration). | A01 Broken Access Control | T1087 Account Discovery (Discovery) |
| LEDGER-003 | Burst of successful debits from one actor. | A06 Insecure Design (no rate limiting) | T1657 Financial Theft (Impact) |
| LEDGER-004 | Many 4xx responses for one actor (probing, fuzzing). Low severity context. | A06 Insecure Design (no rate limiting) | T1595 Active Scanning (Reconnaissance) |

Thresholds are configurable under `ledger.security.*` (see `application.yml`).

## Fighting alert fatigue

1. **Aggregation windows**: rules count events per actor over a sliding window
   instead of alerting on single requests.
2. **Deduplication**: detections with the same fingerprint
   (`rule:actor:ip`) fold into one active finding; `occurrenceCount` grows and
   severity/score only ever go up. A resolved finding is not reused, so a
   recurrence after triage opens a new one.
3. **Risk-based ordering**: findings are sorted by score, not by arrival time.
4. **Measured**: `GET /api/security/summary` reports raw events, findings
   opened, duplicates folded and the resulting noise reduction.

## Enrichment and scoring

```
riskScore = min(100, baseScore(severity) x assetMultiplier x intelMultiplier)
baseScore:        LOW 10, MEDIUM 30, HIGH 60, CRITICAL 90
assetMultiplier:  balance >= 10 000 -> 1.5, >= 1 000 -> 1.2, else 1.0
intelMultiplier:  source IP listed in the threat-intel feed -> 1.5, else 1.0
```

A teaching heuristic, not a standard. The threat-intel feed is a static map in
configuration (`ledger.security.malicious-ips`); the `ThreatIntelProvider`
interface is where a real feed (with caching and timeouts) would plug in.

## Vulnerability prioritisation

`GET /api/security/vulnerabilities` ranks **sample** dependency findings
(fictional CVE-2099-xxxx ids) with:

```
score = cvss/10 x 40 + epss x 30 + 20 (known exploited) + 10 (internet exposed)
score x= 0.4 when the library is not loaded at runtime
```

The same CVSS can land in different tiers depending on exploitation signals and
runtime context. Replace `SampleVulnerabilityCatalog` with an SCA/advisory
feed (for example OSV) to make it real.

## API

| Endpoint | Purpose |
|---|---|
| `GET /api/security/findings?status=&minScore=` | Findings, highest risk first |
| `GET /api/security/findings/{id}` | One finding |
| `POST /api/security/findings/{id}/status` | `{ "status": "ACKNOWLEDGED" \| "RESOLVED" \| "OPEN" }` |
| `GET /api/security/events?limit=&actor=` | Recent raw events (investigation context) |
| `GET /api/security/summary` | Noise metrics |
| `GET /api/security/vulnerabilities` | Prioritised sample vulnerabilities |

## Try it

```bash
cd ledger-api
./gradlew bootRun --args='--spring.profiles.active=demo --ledger.security.trust-forwarded-for=true'
# in another shell
docs/threat-demo.sh
# then open the UI (ng serve) at http://localhost:4200/security
```

`trust-forwarded-for` is only for the demo, so the script can simulate a source
address from the threat-intel feed. Never enable it unless a proxy you control
sets the header.

## Findings about the existing application (not fixed here)

Building the sensor surfaced real weaknesses in the ledger itself. The module
*detects* them; it does not fix them.

- **Broken object level authorisation (OWASP A01).** `GET /accounts/{id}`,
  `/balance`, `/transactions`, `/transactions/{entryId}`, `POST /deposit`,
  `POST /debit` and `GET /quote` never check that the account belongs to the
  caller. Only `exchange` scopes by `userId`. Fix: resolve the account with
  `findByIdAndUserId` (the repository method already exists) and return 404
  otherwise.
- **Client-asserted identity (OWASP A07).** The caller is whatever the
  `X-User-Id` header says, and the Angular interceptor hard-codes `1`. Real
  authentication (session or token) must supply the identity.
- **Unauthenticated security API.** `/api/security/**` follows the rest of the
  app and has no access control.

## Prototype limits

- Events live in a bounded in-memory buffer; findings are persisted. A restart
  clears the noise counters and the event history.
- Processing is serialised in one JVM. Scaling out needs a shared store or
  partitioning by actor (for example a Kafka topic keyed by actor).
- Rules see only what the HTTP layer exposes: no request bodies, so amounts are
  not analysed.

---

# Interview cheat sheet

## OWASP Top 10:2025

An awareness list of the ten most critical web-application risks, updated
every few years from CVE/CWE data plus a practitioner survey.

| # | Category | One-liner |
|---|---|---|
| A01 | Broken Access Control | Users act outside their rights. Now also covers SSRF; BOLA/BFLA called out explicitly. |
| A02 | Security Misconfiguration | Jumped from #5; defaults, open admin consoles, verbose errors. |
| A03 | Software Supply Chain Failures | New; vulnerable or tampered dependencies and build pipelines. |
| A04 | Cryptographic Failures | Weak or missing crypto for data in transit and at rest. |
| A05 | Injection | Untrusted input interpreted as code (SQL, OS command, XSS). |
| A06 | Insecure Design | Missing threat modelling, no rate limits, flawed business logic. |
| A07 | Authentication Failures | Weak login, session and credential handling. |
| A08 | Software or Data Integrity Failures | Trusting unsigned updates, CI/CD artefacts or serialised data. |
| A09 | Logging & Alerting Failures | Attacks happen but nobody sees or is alerted. |
| A10 | Mishandling of Exceptional Conditions | New; errors that fail open or leak internals. |

Say it like this: "BOLA is the most common API flaw: the endpoint checks that
you are logged in, not that the object is yours. Fix it server-side, per object,
by always scoping lookups by the authenticated principal."

## CVE, CVSS, EPSS, KEV, vulnerability management

- **CVE**: a public identifier for one specific vulnerability.
- **CVSS**: severity score 0-10 describing how bad it *could* be. It says
  nothing about whether anyone is exploiting it.
- **EPSS**: probability (0-1) that a CVE is exploited in the wild within about
  30 days.
- **KEV**: a catalogue (CISA) of vulnerabilities known to be exploited now.
- **Vulnerability management** is a loop: discover (SCA/scanners, SBOM) ->
  prioritise (CVSS + EPSS + KEV + exposure + is the code actually loaded) ->
  remediate or mitigate -> verify. The point is to fix what is *reachable and
  exploited* first, not everything with a high CVSS.

## SIEM vs SOAR (and XDR)

- **SIEM**: collects and normalises logs from many sources, correlates them,
  stores them for search and compliance, raises alerts.
- **SOAR**: orchestrates and automates the response with playbooks (enrich an
  IP, open a ticket, isolate a host, notify). It acts on alerts the SIEM raised.
- **XDR**: vendor-integrated detection and response across endpoint, network,
  cloud and identity.
- Dynatrace angle: Grail (data lakehouse) holds observability and security data
  together, DQL queries it, OpenPipeline ingests and enriches it, Security
  Investigator supports investigation, workflows automate response. The value
  claim is security findings *with runtime context*.

## MITRE ATT&CK

A knowledge base of real attacker behaviour: **tactics** (the goal, such as
Initial Access, Discovery, Impact), **techniques** and sub-techniques (the
method, `T1190`), plus groups, software and campaigns. It is behaviour, not
vulnerabilities (that is CVE). Use it to tag detections, measure coverage and
speak a common language with security teams. Current major release at the time
of writing is v20 (October 2026); Enterprise has 15 tactics since v19 split
Defense Evasion into Stealth and Defense Impairment.

## Alert fatigue and noise reduction

Too many low-value alerts train people to ignore all of them. Reduce it by:
aggregating over windows, deduplicating by fingerprint, correlating related
signals into one incident, scoring by context (asset value, exposure, intel),
suppressing known-benign patterns, tuning thresholds from feedback, and
measuring the ratio of events to actionable findings.

## Threat-intel enrichment

Add context to a raw signal so a human or a playbook can decide quickly:
IOC reputation (IP, domain, file hash), geography and ASN, whether the asset is
internet-facing or business-critical, the identity involved, and mapped
ATT&CK technique. Cache lookups, set timeouts, and degrade gracefully when the
feed is down.

## Thirty-second story about this prototype

"I added a small threat-observability module to my ledger project. A sensor
turns API calls into security events, rules map them to OWASP and ATT&CK, an
enrichment step adds threat intel and asset value, and a correlation layer
deduplicates everything into scored findings, so a burst of repeated events
becomes one finding with an occurrence counter. While building it I found that my own API had
object-level authorisation gaps, which is exactly the kind of thing such a
product exists to surface."
