FROM node:24-alpine

WORKDIR /app

COPY package.json .
COPY src/ ./src/

RUN mkdir -p /app/data

EXPOSE 3000

CMD ["npm", "start"]
