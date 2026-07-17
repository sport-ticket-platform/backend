# Microservice API Documentation (Collapsible Template)

This documentation contains the list of the project's endpoints.

---

## General Information
* **Noting:** `nt`

---

## Endpoints List

<details>
<summary><b><code>auth/login-password</code> POST | User Login with Password</b></summary>
<br>

**Description:** Public endpoint

#### Request Body Parameters
| Field Name | Data Type | Required | Description               |
| :--- | :--- |:---------|:--------------------------|
| `identifier` | String | Yes      | phone number or email     |
| `password` | String | Yes      | password     |


#### Responses

**✅ 200 OK | Login Complete**

#### Request Body Parameters
| Field Name | Data Type | Required | Description               |
| :--- | :--- |:---------|:--------------------------|
| `identifier` | String | Yes      | phone number or email     |
| `password` | String | Yes      | password     |



**❌ 400 Bad Request (Input Validation Error)**
```json
{
  "status": "error",
  "message": "Invalid input data."
}
```

**❌ 401 Unauthorized (Invalid Credentials)**
```json
{
  "status": "error",
  "message": "Incorrect username or password."
}
```
</details>

<br>

<details>
<summary><b>🔵 GET <code>/users/{id}</code> - Get User Profile Information</b></summary>
<br>

**Description:** This method retrieves the user's profile information based on their ID. Requires a token (Private).

#### Path Parameters
| Parameter | Data Type | Required? | Description |
| :--- | :--- | :--- | :--- |
| `id` | Long | Yes | Unique user ID in the database |

#### Headers
| Header Name | Value | Description |
| :--- | :--- | :--- |
| `Authorization` | `Bearer <token>` | JWT token received from the login step |

#### Possible Responses

**✅ 200 OK (Success)**
```json
{
  "status": "success",
  "data": {
    "id": 1024,
    "username": "developer_user",
    "email": "user@example.com"
  }
}
```

**❌ 401 Unauthorized (Access Denied)**
```json
{
  "status": "error",
  "message": "You must be authenticated to access this section."
}
```

**❌ 404 Not Found**
```json
{
  "status": "error",
  "message": "User with the provided ID not found."
}
```
</details>