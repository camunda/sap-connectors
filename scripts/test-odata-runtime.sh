#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: bash scripts/test-odata-runtime.sh IMAGE" >&2
  exit 2
fi
for tool in docker curl jq javac jar; do
  if ! command -v "$tool" >/dev/null; then
    echo "ERROR: required command not found: $tool" >&2
    exit 2
  fi
done

container="sap-odata-smoke-$$"
log_file="$(mktemp)"
probe_dir="$(mktemp -d)"
cleanup() {
  docker logs "$container" 2>&1 || true
  docker rm -f "$container" >/dev/null 2>&1 || true
  rm -f "$log_file"
  rm -f "$probe_dir/GuavaRuntimeCheck.class" "$probe_dir/MANIFEST.MF" "$probe_dir/check.jar"
  rmdir "$probe_dir"
}
trap cleanup EXIT

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
javac --release 21 -d "$probe_dir" "$script_dir/GuavaRuntimeCheck.java"
printf 'Premain-Class: GuavaRuntimeCheck\n' >"$probe_dir/MANIFEST.MF"
jar --create --file "$probe_dir/check.jar" --manifest "$probe_dir/MANIFEST.MF" \
  -C "$probe_dir" GuavaRuntimeCheck.class
chmod 755 "$probe_dir"
chmod 644 "$probe_dir/check.jar"

docker run -d --pull=never --name "$container" -p 127.0.0.1::8080 \
  -v "$probe_dir:/opt/guava-check:ro" \
  -e JAVA_TOOL_OPTIONS=-javaagent:/opt/guava-check/check.jar \
  -e CAMUNDA_CLIENT_MODE=selfManaged \
  -e CAMUNDA_CLIENT_AUTH_METHOD=none \
  -e CAMUNDA_CLIENT_GRPCADDRESS=http://127.0.0.1:26500 \
  -e CAMUNDA_CLIENT_RESTADDRESS=http://127.0.0.1:8081 \
  -e CAMUNDA_CONNECTOR_POLLING_ENABLED=false \
  -e CAMUNDA_CONNECTOR_WEBHOOK_ENABLED=false \
  -e CAMUNDA_CONNECTOR_SECRETPROVIDER_CONSOLE_ENABLED=false \
  -e MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true \
  -e 'destinations=[]' \
  "$1" >/dev/null
address="$(docker port "$container" 8080/tcp)"

for attempt in $(seq 1 60); do
  if [[ "$(docker inspect -f '{{.State.Running}}' "$container")" != true ]]; then
    echo "FAIL: OData runtime exited before completing startup" >&2
    exit 1
  fi
  docker logs "$container" >"$log_file" 2>&1
  if grep -q 'PASS: effective runtime Guava classes match fixed 33.7.2-jre' "$log_file" &&
    grep -q 'Started ConnectorRuntimeApplication' "$log_file" &&
    curl -fsS --max-time 3 "http://$address/actuator/health/liveness" 2>/dev/null |
      jq -e '.status == "UP"' >/dev/null; then
    echo "PASS: full OData startup and liveness UP (loopback-only broker; no connectivity check)"
    exit 0
  fi
  sleep 2
done

echo "FAIL: OData runtime did not complete startup with liveness UP within 60 attempts" >&2
exit 1
