# ---- build the Angular app ----
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- serve via nginx (this nginx IS the front door: static + /api proxy) ----
FROM nginx:alpine AS serve
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Angular 17+ (application builder) emits to dist/<project>/browser.
# Adjust the project name if your angular.json differs.
COPY --from=build /app/dist/ledger-web/browser /usr/share/nginx/html
EXPOSE 80
