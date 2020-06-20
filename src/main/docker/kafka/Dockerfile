FROM wurstmeister/kafka:2.12-2.5.0

# prometheus
ADD prometheus-config.yml /opt/kafka/bin/prometheus-config.yml
RUN curl https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.13.0/jmx_prometheus_javaagent-0.13.0.jar --output /opt/kafka/bin/jmx_prometheus_javaagent.jar
RUN chmod +r /opt/kafka/bin/jmx_prometheus_javaagent.jar

# unset KAFKA_OPTS and KAFKA_JMX_OPTS in create-topics.sh scripts to eliminate "address already in use" error because of the javaagent
RUN sed -i 's#while netstat#export KAFKA_OPTS=\nexport KAFKA_JMX_OPTS=\nwhile netstat#g' /usr/bin/create-topics.sh
