FROM eclipse-temurin:25-jdk

WORKDIR /app

COPY . .

RUN javac *.java

CMD ["java", "ChatServer"]
