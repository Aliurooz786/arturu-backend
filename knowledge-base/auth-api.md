# Authentication API Details

## Tool
Postman / Curl

## Description
Used for generating authentication token required for API calls.

## API Endpoint
POST https://api/auth/token

## Curl
curl --location 'https://api/auth/token' \
--header 'Content-Type: application/json' \
--data '{
  "clientId": "abc",
  "clientSecret": "xyz"
}'

## Response
{
  "access_token": "token_value",
  "expires_in": 3600
}

## Usage
Use this token in:
Authorization: Bearer <token>

## Notes
- Token expires every 1 hour
- Regenerate if 401 error occurs

## Tags
auth, token, api, curl