import { defineConfig } from 'vite';
export default defineConfig({server:{proxy:{'/api':'http://localhost:8080','/oauth2':'http://localhost:8080','/login':'http://localhost:8080','/logout':'http://localhost:8080'}}});
