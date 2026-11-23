import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
export default defineConfig({plugins:[vue()],build:{target:'es2020'},server:{proxy:{'/api':process.env.BACKEND_URL||'http://127.0.0.1:8080'}}})
