#!/bin/sh
until curl -s http://elasticsearch:9200/_cluster/health | grep -q '"status":"green"\|"status":"yellow"'; do
  echo "Waiting for Elasticsearch..."
  sleep 2
done

echo "Elasticsearch is ready."

# Delete plain index if it exists (conflicts with data stream)
curl -s -X DELETE "http://elasticsearch:9200/hotelhub-audit-logs"

echo ""
echo "Creating audit index template for Data Stream..."

# Create index template with data_stream
curl -s -X PUT "http://elasticsearch:9200/_index_template/hotelhub-audit-logs" \
  -H "Content-Type: application/json" \
  -d @/etc/elasticsearch/audit-index-template.json

echo ""
echo "Creating Data Stream hotelhub-audit-logs..."

curl -s -X PUT "http://elasticsearch:9200/_data_stream/hotelhub-audit-logs"

echo ""
echo "Audit Data Stream hotelhub-audit-logs created successfully."
