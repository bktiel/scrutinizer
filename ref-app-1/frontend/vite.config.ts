import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
    server: {
        port: 3000,
        strictPort: true,
        hmr: {
            clientPort: 3000,
        },
        proxy: {
            '/api/v1': {
                target: 'http://localhost:8080',
                changeOrigin: true
            }
        }
    },
    plugins: [react()],
})
