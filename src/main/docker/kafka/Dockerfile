FROM confluentinc/cp-kafka:7.3.1-1-ubi8

# kraft
ADD --chown=appuser:appuser update_run.sh /tmp/update_run.sh
RUN chmod a+x /tmp/update_run.sh

# prometheus
ADD --chown=appuser:appuser prometheus-config.yml /var/lib/kafka/prometheus-config.yml
ADD --chown=appuser:appuser https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.13.0/jmx_prometheus_javaagent-0.13.0.jar /var/lib/kafka/jmx_prometheus_javaagent.jar

