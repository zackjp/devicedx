# Lightweight Node.js image (Debian-based)
FROM node:20-bullseye-slim

WORKDIR /workspace

RUN apt-get update && apt-get install -y \
    git \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Install Claude CLI globally inside the container
RUN npm install -g @anthropic-ai/claude-code

