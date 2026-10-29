# BRD: Greeting

## Goal
A service that stores one greeting and returns it.

## User
One caller. No login.

## Behavior
- POST /greetings with {"text": "..."} saves the text and returns an id.
- GET /greetings/{id} returns that text.
- Unknown id returns 404.
- text is required, 1 to 40 characters.

## Out of scope
No accounts, no lists, no updates, no deletes, no search,
no files, no messages queues, no external systems.
