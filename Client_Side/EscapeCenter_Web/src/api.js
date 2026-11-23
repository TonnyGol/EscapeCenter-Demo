// Not set (npm run dev): the API on this computer. Set to "" (the Docker image): the address the site was opened from,
// where nginx or Caddy forwards /api, so a phone on the same Wi-Fi works too.
export const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";
