# UCM Draft Debug Guide

## Tool
Kibana

## Description
This document is used when UCM draft API fails or returns incorrect response.

## Common Errors
- 40072: Failed to fetch ucm draft profile
- 429: Too many requests / throttling

## Kibana Links
https://kibana.ucm/draft-logs
https://kibana.ucm/error-trace

## Steps to Debug
1. Search using requestId
2. Filter by errorCode = 40072
3. Check backend service logs

## Related Curl
curl --location 'https://api/ucm/draft' \
--header 'Authorization: Bearer <token>' \
--header 'x-user-id: <user-id>'

## Notes
- Check DB if draft exists
- Sometimes DAO layer throws generic exception mapped to 429

## Tags
ucm, draft, kibana, error, 429