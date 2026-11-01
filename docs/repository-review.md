# Initial repository review

Baseline: main commit `cbaeb26`, inspected 31 October 2026.

| Finding                                                  | Change                                                                      |
| -------------------------------------------------------- | --------------------------------------------------------------------------- |
| Gateway catch-all stripped Authorization for all clients | Explicit browser, WebSocket and bearer API routes                           |
| Demo APIs returned status text, not scoped data          | Flight fixtures with role/terminal checks; conditional mutation             |
| JWT role converter not explicitly connected              | Explicit authority converter used by each resource server                   |
| Shared token persistence not concretely configured       | Authoritative encrypted Redis authorized-client repository                  |
| Refresh lease lacked write fencing                       | Atomic owner-check-and-save; stale writers rejected                         |
| User block endpoint not checked downstream               | Verified JWT subject block plus token digest checks                         |
| UI was a single unformatted component                    | Shell plus independently built/runtime-mounted remotes                      |
| Realm omitted secrets, audience and usable onboarding    | Generated complete demo realm, roles, users and ATC client                  |
| Infrastructure contained dependencies only               | Application images, Compose/CLI/native/plain K8s/Helm paths                 |
| No tests                                                 | Security, authorization, CSRF and concurrency contracts plus ATC simulation |

The original docs' claims about atomic global logout and zero-gap distributed refresh were not supported by code. The updated docs describe actual guarantees and limits rather than retaining those claims.
