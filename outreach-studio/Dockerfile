# Stage 1: Build
FROM node:20-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build:no-check

# Stage 2: Serve
FROM nginx:stable-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
    && mkdir -p /tmp/client_body /tmp/proxy /tmp/fastcgi /tmp/uwsgi /tmp/scgi \
    && chown -R appuser:appgroup /tmp \
    && chown -R appuser:appgroup /var/cache/nginx \
    && chown -R appuser:appgroup /etc/nginx/conf.d \
    && touch /tmp/nginx.pid \
    && chown appuser:appgroup /tmp/nginx.pid

COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=builder /app/dist /usr/share/nginx/html

USER appuser
EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:80/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
