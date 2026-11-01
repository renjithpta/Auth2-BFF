import { defineConfig } from "vite";
export default defineConfig({
  define: { "process.env.NODE_ENV": JSON.stringify("production") },
  build: {
    lib: {
      entry: "src/remote.jsx",
      formats: ["es"],
      fileName: () => "remoteEntry.js",
    },
    emptyOutDir: true,
  },
});
