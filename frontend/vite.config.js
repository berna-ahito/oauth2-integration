import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// dev server proxy: anything starting with /api, /oauth2, /login, /logout goes to Spring Boot 8080
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // JSON API we’ll add in backend
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      // forward Spring Security endpoints used during OAuth
      '/oauth2': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/login': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/logout': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },
})
