# Multi-stage build for Node.js Express backend
FROM node:20-alpine AS builder

WORKDIR /usr/src/app

COPY package*.json ./
RUN npm ci

COPY . .

FROM node:20-alpine AS production

WORKDIR /usr/src/app

COPY package*.json ./
RUN npm ci --only=production

COPY --from=builder /usr/src/app/server.js ./

# Ensure data file placeholder exists for volume mounting
RUN touch nexus-data.json

USER node

EXPOSE 5263

ENV PORT=5263
ENV NODE_ENV=production

CMD ["node", "server.js"]
