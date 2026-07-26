# Image Upload Service API

Backend API for user authentication, image uploading, AI-generated image tags, public image discovery, and image searching.

## Base URLs

### Local development

```text
http://localhost:8080/api/v1
```

### Production

The production URL will become active after deployment to Render.

```text
https://imageuploadservice.onrender.com/api/v1
```

## Content types

The API uses:

- `application/json` for JSON requests and responses
- `multipart/form-data` for image uploads
- The original image content type when returning image data

---

# Database diagram

```mermaid
flowchart LR
    USERS((
        USERS
        <br/><br/>
        id: BIGSERIAL PK
        <br/>
        username: VARCHAR(50) UNIQUE
        <br/>
        email: VARCHAR(255) UNIQUE
        <br/>
        password_hash: TEXT
        <br/>
        created_at: TIMESTAMP
    ))

    SESSIONS((
        SESSIONS
        <br/><br/>
        id: BIGSERIAL PK
        <br/>
        session_token: UUID UNIQUE
        <br/>
        user_id: BIGINT FK
        <br/>
        expires_at: TIMESTAMP
        <br/>
        created_at: TIMESTAMP
    ))

    IMAGES((
        IMAGES
        <br/><br/>
        id: BIGSERIAL PK
        <br/>
        user_id: BIGINT FK
        <br/>
        title: VARCHAR(255)
        <br/>
        storage_key: TEXT
        <br/>
        content_type: VARCHAR(100)
        <br/>
        file_size: BIGINT
        <br/>
        tags: JSONB
        <br/>
        tagging_status: VARCHAR(20)
        <br/>
        created_at: TIMESTAMP
    ))

    USERS -->|"One user can have many sessions"| SESSIONS
    USERS -->|"One user can upload many images"| IMAGES
```

The application uses PostgreSQL.

- Passwords are stored as bcrypt hashes.
- Opaque session IDs are stored in the `sessions` table.
- Session expiration dates are stored in the database.
- Image files are stored in private object storage.
- Image metadata is stored in the `images` table.
- AI-generated tags are stored in the `tags` JSONB column.
- Deleting a user deletes their database sessions and image records.
- The image service must also remove deleted image files from object storage.

---

# Authentication

The API uses opaque session IDs instead of JWTs.

After a successful login:

1. The server generates a random session ID.
2. The session ID is stored in the database.
3. An expiration date is stored with the session.
4. The session ID is sent to the client in an HTTP-only cookie.

```http
Set-Cookie: SESSION=<opaque-session-id>; HttpOnly; SameSite=Lax; Path=/
```

Authenticated requests must include:

```http
Cookie: SESSION=<opaque-session-id>
```

In production, the cookie also uses the `Secure` attribute:

```http
Set-Cookie: SESSION=<opaque-session-id>; HttpOnly; Secure; SameSite=Lax; Path=/
```

---

# Endpoint summary

| Method | Endpoint | Authentication | Description |
|---|---|---:|---|
| `POST` | `/api/v1/auth/register` | No | Register a new user |
| `POST` | `/api/v1/auth/login` | No | Log in and create a database session |
| `POST` | `/api/v1/auth/logout` | Yes | Delete the current session and log out |
| `GET` | `/api/v1/auth/me` | Yes | Get the authenticated user |
| `POST` | `/api/v1/images` | Yes | Upload an image of up to 10 MB |
| `GET` | `/api/v1/images/mine` | Yes | Get all images uploaded by the current user |
| `GET` | `/api/v1/images/{imageId}` | No | Get image metadata |
| `GET` | `/api/v1/images/{imageId}/content` | No | Proxy image data from private object storage |
| `DELETE` | `/api/v1/images/{imageId}` | Yes | Delete an image owned by the current user |
| `GET` | `/api/v1/public/images` | No | Get the latest 50 images |
| `GET` | `/api/v1/public/images/search?q={query}` | No | Search images using free text |

---

# Authentication endpoints

## Register a user

Creates a new user account.

```http
POST /api/v1/auth/register
Content-Type: application/json
```

### Request body

```json
{
  "username": "john",
  "email": "john@example.com",
  "password": "securePassword123"
}
```

### Validation

- `username` is required
- `username` must be unique
- `email` must be a valid email address
- `email` must be unique
- `password` must contain at least 8 characters
- The password is hashed with bcrypt before storage

### Success response

```http
201 Created
```

```json
{
  "id": 1,
  "username": "john",
  "email": "john@example.com",
  "createdAt": "2026-07-26T14:30:00Z"
}
```

### Error responses

- `400 Bad Request` — validation failed
- `409 Conflict` — username or email already exists

---

## Log in

Authenticates a user and creates an opaque database session.

```http
POST /api/v1/auth/login
Content-Type: application/json
```

### Request body

```json
{
  "email": "john@example.com",
  "password": "securePassword123"
}
```

### Success response

```http
200 OK
Set-Cookie: SESSION=<opaque-session-id>; HttpOnly; SameSite=Lax; Path=/
```

```json
{
  "id": 1,
  "username": "john",
  "email": "john@example.com"
}
```

### Error responses

- `400 Bad Request` — validation failed
- `401 Unauthorized` — email or password is incorrect

---

## Log out

Deletes the current database session and clears the session cookie.

```http
POST /api/v1/auth/logout
Cookie: SESSION=<opaque-session-id>
```

### Success response

```http
204 No Content
Set-Cookie: SESSION=; Max-Age=0; HttpOnly; SameSite=Lax; Path=/
```

### Error response

- `401 Unauthorized` — session is missing, invalid, or expired

---

## Get the current user

Returns the authenticated user.

```http
GET /api/v1/auth/me
Cookie: SESSION=<opaque-session-id>
```

### Success response

```http
200 OK
```

```json
{
  "id": 1,
  "username": "john",
  "email": "john@example.com"
}
```

### Error response

- `401 Unauthorized` — session is missing, invalid, or expired

---

# Image endpoints

## Upload an image

Uploads an image for the authenticated user.

The image file is stored in private object storage. Its metadata is stored in PostgreSQL.

AI tagging runs in the background, so the endpoint returns `202 Accepted`.

```http
POST /api/v1/images
Content-Type: multipart/form-data
Cookie: SESSION=<opaque-session-id>
```

### Form fields

| Field | Type | Required | Description |
|---|---|---:|---|
| `file` | File | Yes | JPEG, PNG, or WebP image |
| `title` | String | No | Optional image title |

### Validation

- The user must be authenticated
- The file must be an image
- The maximum file size is 10 MB
- The content type must be supported
- Empty files are rejected
- AI-generated tags are stored as JSONB

### Success response

```http
202 Accepted
```

```json
{
  "id": 42,
  "title": "Beach sunset",
  "contentType": "image/jpeg",
  "fileSize": 2456789,
  "taggingStatus": "PENDING",
  "tags": null,
  "imageUrl": "/api/v1/images/42/content",
  "createdAt": "2026-07-26T14:30:00Z"
}
```

### Error responses

- `400 Bad Request` — file is missing, empty, or invalid
- `401 Unauthorized` — authentication is required
- `413 Payload Too Large` — file exceeds 10 MB
- `415 Unsupported Media Type` — file type is unsupported
- `500 Internal Server Error` — upload processing failed
- `502 Bad Gateway` — object-storage service failed

---

## Get the current user’s images

Returns all images uploaded by the authenticated user.

```http
GET /api/v1/images/mine
Cookie: SESSION=<opaque-session-id>
```

### Success response

```http
200 OK
```

```json
[
  {
    "id": 42,
    "title": "Beach sunset",
    "contentType": "image/jpeg",
    "fileSize": 2456789,
    "taggingStatus": "COMPLETED",
    "tags": {
      "objects": ["palm tree", "sun", "sea"],
      "tags": ["sunset", "beach scene"],
      "colors": ["orange", "pink", "purple"]
    },
    "imageUrl": "/api/v1/images/42/content",
    "createdAt": "2026-07-26T14:30:00Z"
  }
]
```

### Error response

- `401 Unauthorized` — authentication is required

---

## Get image metadata

Returns information about one image.

```http
GET /api/v1/images/{imageId}
```

### Success response

```http
200 OK
```

```json
{
  "id": 42,
  "title": "Beach sunset",
  "contentType": "image/jpeg",
  "fileSize": 2456789,
  "taggingStatus": "COMPLETED",
  "tags": {
    "objects": ["palm tree", "sun", "sea"],
    "tags": ["sunset", "beach scene"],
    "colors": ["orange", "pink", "purple"]
  },
  "imageUrl": "/api/v1/images/42/content",
  "createdAt": "2026-07-26T14:30:00Z"
}
```

### Error response

- `404 Not Found` — image does not exist

---

## View image content

Returns the actual image data.

The server retrieves the image from the private object-storage bucket and returns it to the client. The private object-storage URL is never exposed.

```http
GET /api/v1/images/{imageId}/content
```

### Success response

```http
200 OK
Content-Type: image/jpeg
Content-Length: 2456789
```

The response body contains binary image data.

### Error responses

- `404 Not Found` — image does not exist
- `502 Bad Gateway` — image could not be retrieved from object storage

---

## Delete an image

Deletes an image owned by the authenticated user.

The image record and the object-storage file are both deleted.

```http
DELETE /api/v1/images/{imageId}
Cookie: SESSION=<opaque-session-id>
```

### Success response

```http
204 No Content
```

### Error responses

- `401 Unauthorized` — authentication is required
- `403 Forbidden` — the image belongs to another user
- `404 Not Found` — image does not exist
- `502 Bad Gateway` — image could not be deleted from object storage

---

# Public image endpoints

## Get the latest images

Returns up to 50 images for the home page.

Images are ordered from newest to oldest.

```http
GET /api/v1/public/images
```

### Success response

```http
200 OK
```

```json
[
  {
    "id": 42,
    "title": "Beach sunset",
    "taggingStatus": "COMPLETED",
    "tags": {
      "objects": ["palm tree", "sun", "sea"],
      "tags": ["sunset", "beach scene"],
      "colors": ["orange", "pink", "purple"]
    },
    "imageUrl": "/api/v1/images/42/content",
    "createdAt": "2026-07-26T14:30:00Z"
  }
]
```

---

## Search images

Searches image titles and AI-generated tags using free text.

```http
GET /api/v1/public/images/search?q=sunset
```

### Query parameters

| Parameter | Type | Required | Description |
|---|---|---:|---|
| `q` | String | Yes | Free-text search query |

### Example request

```http
GET /api/v1/public/images/search?q=car
```

### Success response

```http
200 OK
```

```json
[
  {
    "id": 50,
    "title": "Red car",
    "taggingStatus": "COMPLETED",
    "tags": {
      "objects": ["car", "road", "tree"],
      "tags": ["daytime", "outdoor", "driving"],
      "colors": ["red", "green", "gray"]
    },
    "imageUrl": "/api/v1/images/50/content",
    "createdAt": "2026-07-26T15:00:00Z"
  }
]
```

If no images match the search query:

```http
200 OK
```

```json
[]
```

### Error response

- `400 Bad Request` — search query is missing or blank

---

# Standard error response

All API errors use a consistent response structure.

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Email must be valid",
  "path": "/api/v1/auth/register",
  "timestamp": "2026-07-26T14:30:00Z"
}
```

## Common status codes

| Status | Meaning |
|---:|---|
| `200` | Request completed successfully |
| `201` | Resource created successfully |
| `202` | Request accepted for background processing |
| `204` | Request completed with no response body |
| `400` | Invalid request or validation failure |
| `401` | Authentication is required |
| `403` | Authenticated user does not have permission |
| `404` | Resource was not found |
| `409` | Resource already exists |
| `413` | Uploaded file is too large |
| `415` | Uploaded file type is unsupported |
| `500` | Unexpected server error |
| `502` | Object-storage service failed |

---

# AI-generated tag format

AI-generated tags are stored in the `images.tags` JSONB column.

```json
{
  "objects": [
    "palm tree",
    "sun",
    "sea",
    "beach",
    "boat",
    "sand"
  ],
  "tags": [
    "sunset",
    "beach scene",
    "silhouette",
    "tropical",
    "reflection on water"
  ],
  "colors": [
    "orange",
    "pink",
    "purple"
  ]
}
```

The `colors` array contains no more than three values selected from these eleven basic English colors:

```text
black
white
red
green
yellow
blue
brown
orange
pink
purple
gray
```