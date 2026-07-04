# ─── Stage 1: Build React app ─────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm ci --silent

# Copy source and build
COPY . .
RUN npm run build

# ─── Stage 2: Serve with Nginx ────────────────────────────
# Nginx is a high-performance web server
# Much better than running 'npm start' in production
FROM nginx:alpine

# Copy built React files to Nginx web root
COPY --from=builder /app/dist /usr/share/nginx/html

# Custom Nginx config for React Router (client-side routing)
# Without this, refreshing /transactions gives a 404
RUN echo 'server { \
  listen 80; \
  location / { \
    root /usr/share/nginx/html; \
    index index.html; \
    try_files $uri $uri/ /index.html; \
  } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s \
  CMD wget -q --spider http://localhost:80 || exit 1