# SQL Injection 시도
curl -X POST "http://localhost:5001/analyze" -H "Content-Type: application/json" -d '{"url": "/api/users?id=1'\'' OR 1=1--", "method": "GET", "headers": {"User-Agent": "AttackBot"}, "body": ""}'

sleep 0.6
# XSS 공격 시도
curl -X POST "http://localhost:5001/analyze" -H "Content-Type: application/json" -d '{"url": "/api/comment", "method": "POST", "headers": {"Content-Type": "application/json"}, "body": "{\"comment\": \"<script>alert('\''XSS'\'')</script>\"}"}'

sleep 0.6
# Path Traversal 공격 시도
curl -X POST "http://localhost:5001/analyze" -H "Content-Type: application/json" -d '{"url": "/api/file?path=../../../etc/passwd", "method": "GET", "headers": {}, "body": ""}'

sleep 0.6
# Command Injection 시도
curl -X POST "http://localhost:5001/analyze" -H "Content-Type: application/json" -d '{"url": "/api/ping", "method": "POST", "headers": {"Content-Type": "application/json"}, "body": "{\"host\": \"8.8.8.8; rm -rf /\"}"}'

sleep 0.6
# LDAP Injection 시도
curl -X POST "http://localhost:5001/analyze" -H "Content-Type: application/json" -d '{"url": "/api/search", "method": "POST", "headers": {"Content-Type": "application/json"}, "body": "{\"filter\": \"(|(cn=*)(userPassword=*))\", \"username\": \"admin\"}"}'

sleep 0.6
# Header Injection 시도
curl -X POST "http://localhost:5001/analyze" -H "Content-Type: application/json" -d '{"url": "/api/redirect", "method": "GET", "headers": {"Host": "evil.com", "X-Forwarded-For": "127.0.0.1\r\nEvil-Header: injected"}, "body": ""}'
sleep 0.6