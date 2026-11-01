# Implemented API, exception and logging contracts

## Naming

Domain REST: `/api/v1/{plural-resources}`. Browser REST: `/bff/api/v1/{plural-resources}`. Native WebSocket: `/bff/ws/v1`. Paths lowercase/kebab-case; JSON lowerCamelCase. Version identifies a breaking contract, not an image release. IDs are opaque fixture IDs in this demo. No credentials, service implementation names or get/update verbs in URLs.

## Flight operations

| Method | Path                 | Success        | Requirements                                   |
| ------ | -------------------- | -------------- | ---------------------------------------------- |
| GET    | /api/v1/flights      | 200            | FLIGHT_VIEW + terminals scope                  |
| GET    | /api/v1/flights/{id} | 200 + ETag     | Object in scope; concealed 404 otherwise       |
| PATCH  | /api/v1/flights/{id} | 200 + new ETag | FLIGHT_UPDATE, scope, If-Match, allowed status |

`terminalId=T1` is an optional filter; asking for an unauthorized terminal returns 403. Unknown IDs return 404. Collections are bounded to the two fixture rows; production cursor pagination/export jobs are not implemented. Requests cannot assign terminal/airline/roles by PATCH. A departed flight cannot be updated.

```json
{
  "data": [
    {
      "flightId": "flight-001",
      "flightNumber": "SN1234",
      "airportId": "BRU",
      "terminalId": "T1",
      "airlineCode": "SN",
      "status": "SCHEDULED",
      "version": 1
    }
  ],
  "meta": { "count": 1 }
}
```

PATCH example: `{"status":"BOARDING"}`, Content-Type application/json, If-Match: "1". This is a typed partial-update contract, not JSON Merge Patch. Missing If-Match → 428; stale → 412; invalid/unknown field → 400; invalid state → 409. Version checking is atomic only inside one reference service process.

## Errors

RFC 9457 `application/problem+json`: type/title/status/detail/instance with code and requestId extensions; traceId when available. Clients branch on status/code, not a human message. Tokens/stack traces are never returned. MVC framework, controller and security boundaries have separate handlers; gateway has its own reactive security boundary.

```json
{
  "type": "urn:aobp:problem:access-denied",
  "title": "Forbidden",
  "status": 403,
  "detail": "Terminal is outside assigned scope.",
  "instance": "urn:uuid:<occurrence>",
  "code": "ACCESS_DENIED",
  "requestId": "<safe-request-reference>"
}
```

Headers: bearer Authorization for API calls, cookie for BFF only, X-Request-ID for support, W3C traceparent for distributed tracing, ETag/If-Match for update concurrency. Sensitive responses use no-store. Do not return 200 for failed HTTP operations. Login redirects occur only on browser login endpoints, not external machine API errors.

## Logs

Spring structured Logstash JSON to stdout, one object per event. Include service/release configuration, severity, event.name, request.id, trace/span context, method/status, elapsed duration and safe resource references. Request completion records do not log cookies, Authorization, raw bodies or query strings. Flight update event includes pseudonymous subject for demo audit visibility. Stack trace is private at unexpected-error owner, not repeated at every layer.

Diagnostic logging is not a durable compliance audit store. This reference logs business changes; transactional persistent audit/outbox is a production gate. Never use DEBUG HTTP wire logging with real credentials.
