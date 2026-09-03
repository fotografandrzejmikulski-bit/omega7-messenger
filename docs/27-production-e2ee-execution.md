# Ω7 — Production E2EE execution gate

## Status

APK generation is intentionally disabled in CI until the production gate is closed.

The current branch is an implementation branch, not a release artifact branch.

## Completed in this stage

- Relay rejects startup without required production secrets.
- Relay limits the active group to 7 devices.
- Invitation consumption is one-time and transactional.
- Device registration stores the Signal public bundle.
- One-time prekeys have a dedicated PostgreSQL table.
- Registration/bootstrap can seed one-time prekeys supplied in `preKeys` or legacy `preKeyId` + `preKey` fields.
- Authenticated prekey replenishment endpoint exists at `POST /v1/keys/prekeys`.
- Bundle retrieval consumes at most one available one-time prekey transactionally using row locking.
- Message submission rejects inactive/nonexistent recipients.
- Auth tokens are validated as 32-byte URL-safe bearer tokens before database authorization.
- Client control-plane code exists for HTTPS bundle retrieval and prekey upload.
- Signal E2EE remains implemented through libsignal rather than custom cryptographic primitives.

## Still required before release

1. Wire provisioning and bundle discovery into the Android UI.
2. Persist relay URL and device auth token in encrypted local state.
3. Complete device registration from the QR approval flow.
4. Establish sessions from server-fetched bundles and handle one-time-prekey exhaustion.
5. Connect send/sync to the real E2EE engine.
6. Implement durable offline queue and bounded retry semantics.
7. Implement device revocation plus group/session rekey protocol.
8. Implement encrypted attachments.
9. Implement metadata-safe push notifications.
10. Add relay rate limiting and abuse controls.
11. Add physical multi-device tests, including 7-device progressive enrollment.
12. Add replay, malformed-input, concurrency, and fuzzing tests.
13. Complete backup/recovery and device replacement behavior.
14. Complete independent cryptographic/security review.
15. Perform a clean release build, signing, SBOM and release-integrity verification only after all preceding gates pass.

## Security rule

The project must not be described as production-ready merely because CI compiles code or produces an APK. The production gate requires successful verification of the complete end-to-end protocol and its operational controls.

The multi-device model follows the Signal Sesame design principle that each physical device has its own identity/session state and that asynchronous delivery must tolerate offline devices, reordering, duplication and state loss.
