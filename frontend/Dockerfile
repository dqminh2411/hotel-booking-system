FROM node:20-alpine
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
ARG VITE_API_BASE_URL=http://localhost:8080
ARG VITE_GOOGLE_CLIENT_ID
ARG VITE_GOOGLE_AUTH_PATH=/api/auth/google
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_GOOGLE_CLIENT_ID=${VITE_GOOGLE_CLIENT_ID}
ENV VITE_GOOGLE_AUTH_PATH=${VITE_GOOGLE_AUTH_PATH}
CMD ["npm", "run", "dev", "--", "--host"]

# FROM nginx:1.27-alpine
# COPY nginx.conf /etc/nginx/conf.d/default.conf
# COPY --from=builder /app/dist /usr/share/nginx/html
# EXPOSE 3000
