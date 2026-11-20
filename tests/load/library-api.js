import http from "k6/http";
import { check, group, sleep } from "k6";

const BASE_URL = (__ENV.API_BASE_URL || "http://localhost:8080").replace(/\/$/, "");
const PROFILE = __ENV.K6_PROFILE || "smoke";

const profiles = {
  smoke: [{ duration: "10s", target: 1 }],
  load: [
    { duration: "30s", target: 5 },
    { duration: "1m", target: 5 },
    { duration: "30s", target: 0 },
  ],
  stress: [
    { duration: "30s", target: 10 },
    { duration: "1m", target: 25 },
    { duration: "30s", target: 0 },
  ],
};

if (!profiles[PROFILE]) {
  throw new Error(`K6_PROFILE inválido: ${PROFILE}. Use smoke, load ou stress.`);
}

const isLocalTarget = /^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/i.test(BASE_URL);
if (!isLocalTarget && __ENV.K6_ALLOW_REMOTE !== "true") {
  throw new Error(
    "Teste de carga bloqueado para host remoto. Use API_BASE_URL local; " +
    "um alvo remoto exige K6_ALLOW_REMOTE=true e autorização explícita.",
  );
}

export const options = {
  scenarios: {
    api_read_workload: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: profiles[PROFILE],
      gracefulRampDown: "10s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<1500"],
  },
};

const jsonHeaders = { headers: { "Content-Type": "application/json" } };

function authenticate(email, password, profileName) {
  const response = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ email, password }),
    jsonHeaders,
  );

  const valid = check(response, {
    [`${profileName} login returns HTTP 200`]: (res) => res.status === 200,
    [`${profileName} login returns access token`]: (res) => Boolean(res.json("accessToken")),
  });

  if (!valid) {
    throw new Error(
      `${profileName} authentication failed (HTTP ${response.status}). ` +
      "Check the local seed and the credentials supplied through environment variables.",
    );
  }

  return response.json("accessToken");
}

export function setup() {
  if (!isLocalTarget) {
    console.warn("Remote load target explicitly enabled. Confirm this is an isolated test environment.");
  }

  const adminToken = authenticate(
    __ENV.ADMIN_EMAIL || "admin@library.com",
    __ENV.ADMIN_PASSWORD || "Admin123!",
    "ADMIN",
  );
  const userToken = authenticate(
    __ENV.USER_EMAIL || "user@library.com",
    __ENV.USER_PASSWORD || "User123!",
    "USER",
  );

  return { adminToken, userToken };
}

function get(path, token, label) {
  const response = http.get(`${BASE_URL}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
    tags: { endpoint: label },
  });

  check(response, {
    [`${label} returns HTTP 200`]: (res) => res.status === 200,
  });
}

export default function (tokens) {
  group("catalog reads", () => {
    get("/books?page=0&size=20&sort=title,asc", tokens.userToken, "books");
    get("/authors?page=0&size=20&sort=name,asc", tokens.userToken, "authors");
  });

  group("user circulation reads", () => {
    get("/loans/my?page=0&size=20&sort=requestDate,desc", tokens.userToken, "my-loans");
    get("/fines/my?page=0&size=20&sort=createdAt,desc", tokens.userToken, "my-fines");
  });

  group("staff circulation reads", () => {
    get("/loans?page=0&size=20&sort=requestDate,desc", tokens.adminToken, "all-loans");
    get("/fines?page=0&size=20&sort=createdAt,desc", tokens.adminToken, "all-fines");
  });

  sleep(1);
}
