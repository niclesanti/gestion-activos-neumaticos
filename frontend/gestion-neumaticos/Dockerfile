# syntax=docker/dockerfile:1

FROM node:22-alpine AS build
WORKDIR /workspace

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Vite incrusta las variables VITE_* en el bundle en tiempo de build.
ARG VITE_API_URL=http://localhost:8080
ENV VITE_API_URL=$VITE_API_URL

RUN npm run build \
    && node scripts/nginx-security-headers.mjs > security-headers.conf \
    # _headers es para Cloudflare: nginx no lo usa y no hace falta servirlo.
    && rm dist/_headers

# Imagen sin root: nginx corre como el usuario "nginx" en el puerto 8080.
FROM nginxinc/nginx-unprivileged:1.30-alpine AS final

COPY --from=build /workspace/dist /usr/share/nginx/html
COPY --from=build /workspace/security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080
