FROM node:20-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm install

COPY . .

CMD ["npm", "run", "dev", "--", "--host"]

# ARG VITE_API_URL=http://localhost:8080
# ENV VITE_API_URL=${VITE_API_URL}
# RUN npm run build

# serve html file in production

# FROM nginx:1.27-alpine
# COPY nginx.conf /etc/nginx/conf.d/default.conf
# COPY --from=builder /app/dist /usr/share/nginx/html
# EXPOSE 3000
