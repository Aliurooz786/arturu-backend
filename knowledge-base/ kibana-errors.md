# Kibana Common Errors Guide

## Tool
Kibana

## Description
General reference for common errors observed in logs.

## Error Types

### 429 Too Many Requests
- Cause: Rate limit exceeded
- Fix:
  - Reduce API calls
  - Check retry logic

### 500 Internal Server Error
- Cause: Backend failure
- Fix:
  - Check service logs
  - Verify DB connectivity

### 400 Bad Request
- Cause: Invalid input
- Fix:
  - Validate payload
  - Check mandatory fields

## Kibana Links
https://kibana/common-errors
https://kibana/service-logs

## Notes
- Always use requestId for tracing
- Prefer recent logs

## Tags
kibana, errors, 429, 500, logs