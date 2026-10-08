# Multi-stage build for lightweight deployment
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN mkdir -p bin && javac -encoding UTF-8 -d bin src/com/library/Main.java src/com/library/model/*.java src/com/library/service/*.java src/com/library/util/*.java src/com/library/server/*.java

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/bin ./bin
COPY --from=build /app/web ./web

ENV PORT=8080
EXPOSE 8080

CMD ["java", "-cp", "bin", "com.library.Main"]
