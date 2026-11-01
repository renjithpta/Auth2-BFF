import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  workers: 1,
  timeout: 60000,
  use: { baseURL: 'http://localhost:8088', headless: true },
  reporter: 'list',
});
