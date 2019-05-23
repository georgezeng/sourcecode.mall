FROM java:8
ARG project
ENV artifact=${project}
RUN echo "Asia/Shanghai" > /etc/timezone
ADD ${project}.jar ./${project}.jar
CMD java -jar ${artifact}.jar -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=2345
EXPOSE 8080
EXPOSE 2345