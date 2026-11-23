# Java 8 运行时（temurin 是 openjdk:8-jre 的现行替代）
FROM eclipse-temurin:8-jre-jammy
WORKDIR /app
ENV TZ=Asia/Shanghai JAVA_OPTS="-Xms512m -Xmx1g"
COPY leomall.jar leomall.jar
# sh -c 展开 JAVA_OPTS；"$@" 透传 compose command 里的 --xxx 启动参数
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar leomall.jar \"$@\"", "sh"]