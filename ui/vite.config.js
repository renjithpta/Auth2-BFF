import { defineConfig } from "vite";
export default defineConfig({
  server: {
    port: 8088,
    strictPort: true,
    proxy: {
      "/bff": { target: "http://localhost:8080", ws: true },
      "/oauth2": "http://localhost:8080",
      "/login": "http://localhost:8080",
      "/logout": "http://localhost:8080",
      "/mfes/flight": {
        target: "http://localhost:8090",
        rewrite: (p) => p.replace("/mfes/flight", ""),
      },
      "/mfes/workflow": {
        target: "http://localhost:8093",
        rewrite: (p) => p.replace("/mfes/workflow", ""),
      },
    },
  },
});
