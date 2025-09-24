###############################################
# CAP bookshop test image (for unit tests only)
# This image is intentionally NOT following normal
# publishing/tagging conventions. It is built locally
# and consumed by Testcontainers during the odata
# connector test phase. It is never pushed.
###############################################

FROM node:24-trixie

WORKDIR /app

COPY . .
RUN npm install

EXPOSE 4004
ENV PORT=4004

# No HEALTHCHECK here: readiness is handled by Testcontainers
# via an HTTP wait strategy on /health.

CMD ["npm", "start"]
