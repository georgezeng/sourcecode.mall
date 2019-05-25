FROM java:8
ARG project
ENV artifact=${project}
RUN echo "Asia/Shanghai" > /etc/timezone
ADD ${project}.jar ./${project}.jar
CMD java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000 -jar ${artifact}.jar
EXPOSE 8080
EXPOSE 8000