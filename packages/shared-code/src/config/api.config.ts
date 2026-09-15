declare const __API_BASE_URL__: string | undefined;

const DEFAULT_API_BASE_URL =
  typeof window !== "undefined" && ["localhost", "127.0.0.1"].includes(window.location.hostname)
    ? "http://localhost:8080/api/v1"
    : "https://kineticstore.online/api/v1";

export const API_BASE_URL =
  typeof __API_BASE_URL__ !== "undefined" && __API_BASE_URL__
    ? __API_BASE_URL__
    : DEFAULT_API_BASE_URL;
