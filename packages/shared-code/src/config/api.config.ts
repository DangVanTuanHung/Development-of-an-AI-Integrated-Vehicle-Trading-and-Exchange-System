declare const __API_BASE_URL__: string | undefined;

const DEFAULT_API_BASE_URL =
  typeof window !== "undefined" && ["localhost", "127.0.0.1"].includes(window.location.hostname)
    ? "http://localhost:8080/api/v1"
    : "https://kineticstore.online/api/v1";

const configuredBaseUrl =
  typeof __API_BASE_URL__ !== "undefined" && __API_BASE_URL__
    ? __API_BASE_URL__
    : DEFAULT_API_BASE_URL;

// Loopback aliases are different sites for SameSite cookies. Keep the configured
// backend port/path, but use the host actually opened in the browser.
export const API_BASE_URL = (() => {
  if (typeof window === "undefined") return configuredBaseUrl;
  const loopback = new Set(["localhost", "127.0.0.1"]);
  const url = new URL(configuredBaseUrl, window.location.origin);
  if (loopback.has(window.location.hostname) && loopback.has(url.hostname)) {
    url.hostname = window.location.hostname;
  }
  return url.toString().replace(/\/$/, "");
})();
