FROM confluentinc/cp-kafka:6.1.1-2-ubi8

# prometheus
ADD --chown=appuser:appuser prometheus-config.yml /var/lib/kafka/prometheus-config.yml
ADD --chown=appuser:appuser https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.13.0/jmx_prometheus_javaagent-0.13.0.jar /var/lib/kafka/jmx_prometheus_javaagent.jar

