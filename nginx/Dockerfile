FROM node:20-alpine AS web-builder

WORKDIR /app
COPY nexora-web/package*.json ./
RUN npm ci

COPY nexora-web ./
ARG VITE_API_URL=/api
ENV VITE_API_URL=${VITE_API_URL}
RUN npm run build

FROM nginx:1.27-alpine

COPY nginx/docker.conf /etc/nginx/conf.d/default.conf
COPY --from=web-builder /app/dist /usr/share/nginx/html

EXPOSE 8080
