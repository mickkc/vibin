# Vibin' Bot Disclosure

> This document explains what and how HTTP requests are automatically performed by Vibin'.
> It is intended for the administrators of those services in case of violations of TOS or other inquiries.
> 
> Last updated: `November 7, 2026`

Vibin' is free software. **Anyone may download, run, and host their own instance**.
We do not operate any central Vibin' instance (except a read-only demo that does not allow using the features described
below) or bot infrastructure.

That means:
- Every request you receive comes from an independent operator, on their own infrastructure and IP address.
- We can't see, change, or stop any instance.
- We can't provide any IP ranges.

However, if an instance violates your terms, contact us at <contact@mickkc.dev>. We will acknowledge your request within
2 business days and fix it in the next upstream release where feasible.

Any request matching `Vibin-Metadata-Bot/*` or `Vibin-Scrobbling-Bot/*` identifies this software or a modified copy of it.
We can't verify that a request came from unmodified upstream code.

## Services

Used services currently include:

- Metadata
  - Deezer
  - iTunes
  - Last.fm
  - LrcLib
  - Spotify
  - TheAudioDB
- Scrobbling
  - Last.fm

This list may change between releases.

## Rate limits and backoff

Instances send requests sequentially, honor `429` and `503` together with `Retry-After`, and do not retry in a tight loop.
**If an instance ignores `Retry-After` or exceeds a provider's limit, that is a bug in the software, please report it!**

## Metadata

Current `User-Agent` value: `Vibin-Metadata-Bot/0.0.15 (Ktor/3.2.3) +https://codeberg.org/mickkc/vibin/raw/branch/main/BOT_DISCLOSURE.md`

The `Vibin-Metadata-Bot` is used to search for tracks, artists, albums and lyrics.
It is run manually by the user of an instance, either once via the search endpoint, or multiple times while a library is
being scanned.

Sent data:
- Search query
- The user's authentication details (e.g. API keys) when necessary.

## Scrobbling

Current `User-Agent` value: `Vibin-Scrobbling-Bot/0.0.15 (Ktor/3.2.3) +https://codeberg.org/mickkc/vibin/raw/branch/main/BOT_DISCLOSURE.md`

The `Vibin-Scrobbling-Bot` is used to scrobble played tracks to different services.
It is run whenever the user finishes or skips a track, unless provider-specific rules (e.g. must play at least half of
the track) prevent it.

It also may update the currently playing track, if the service allows it.
This happens every time the user starts or resumes a track, and is limited to one request per user and track every 5 seconds.

Sent data:
- Track title, duration, artist and album names
- Timestamp when the track was listened to
- The users' authentication details (e.g. API keys) when necessary.

## Reporting

To create a report, email [contact@mickkc.dev](mailto:contact@mickkc.dev) and include the following information:

- Source IPs
- Timestamps (UTC where possible)
- User-Agents
- Endpoints and request volume

---

https://vibin.rip
