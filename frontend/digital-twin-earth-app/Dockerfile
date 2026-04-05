# ─────────────────────────────────────────────────────────────
# Stage 1 — Build
# ─────────────────────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

# Install dependencies first (cached layer)
COPY package*.json ./
RUN npm ci

# Copy source and build for production
COPY . .
RUN npm run build -- --configuration production

# ─────────────────────────────────────────────────────────────
# Stage 2 — Serve
# ─────────────────────────────────────────────────────────────
FROM nginx:1.25-alpine AS runner

RUN rm -rf /usr/share/nginx/html/*

# Angular output + Cesium assets (CopyPlugin writes cesium/ into dist/digital-twin-earth-app/)
COPY --from=builder /app/dist/digital-twin-earth-app /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
