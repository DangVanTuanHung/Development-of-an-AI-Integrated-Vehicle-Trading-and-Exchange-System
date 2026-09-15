import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";
import path from "node:path";

const root = fileURLToPath(new URL("../", import.meta.url));
function docker(args, quiet = false) {
  return new Promise((resolve, reject) => {
    const child = spawn("docker", args, {
      cwd: path.join(root, "backend"), stdio: quiet ? "ignore" : "inherit", windowsHide: true
    });
    child.once("error", reject);
    child.once("exit", (code, signal) => {
      if (code === 0) resolve();
      else reject(new Error("docker " + args.join(" ") + " failed (" + (signal || code) + ")."));
    });
  });
}

try {
  try {
    await docker(["info"], true);
    await docker(["compose", "version"], true);
  } catch {
    throw new Error("Hay cai va mo Docker Desktop, doi Docker san sang, roi chay lai npm run dev.");
  }
  console.log("\n[MOTIONX] Khoi dong PostgreSQL, Redis, MinIO va backend...");
  console.log("[MOTIONX] Lan dau tai images va build Java co the mat vai phut.\n");
  await docker(["compose", "up", "-d", "--build", "--wait", "--wait-timeout", "180",
    "postgres", "redis", "minio", "backend"]);
  // This command always connects the web app to the local backend.
  process.env.VITE_API_BASE_URL = "http://localhost:8080/api/v1";
  process.chdir(path.join(root, "packages", "web"));
  const { createServer } = await import("vite");
  const server = await createServer({ server: { host: "127.0.0.1" } });
  await server.listen();
  console.log("\n[MOTIONX] Backend: http://localhost:8080/api/v1");
  server.printUrls();
  console.log("[MOTIONX] Ctrl+C dung web. npm run dev:stop dung Docker (giu du lieu).\n");
  let stopping = false;
  async function stop() {
    if (stopping) return;
    stopping = true;
    await server.close();
    process.exit(0);
  }
  process.on("SIGINT", stop);
  process.on("SIGTERM", stop);
} catch (error) {
  console.error("\n[MOTIONX] " + error.message);
  console.error("Kiem tra log: docker compose -f backend/docker-compose.yml logs --tail=80");
  process.exitCode = 1;
}
