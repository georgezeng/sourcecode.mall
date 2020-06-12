FROM openjdk:latest
ARG project
ENV artifact=${project}
RUN echo "Asia/Shanghai" > /etc/timezone && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
ADD ${project}.jar ./${project}.jar
#CMD java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000 -jar ${artifact}.jar
CMD java -jar ${artifact}.jar
EXPOSE 8080
EXPOSE 8000