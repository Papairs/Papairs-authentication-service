# Development stage for Vue.js frontend
FROM node:18-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install ALL dependencies (including devDependencies)
RUN npm install --include=dev

# Copy application source
COPY . .

# Expose port 3000
EXPOSE 3000

# Start development server
CMD ["npm", "run", "serve"]
