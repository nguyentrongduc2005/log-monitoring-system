.PHONY: infra-up infra-down backend frontend api build test lint clean

infra-up:
	docker compose up -d postgres clickhouse redis kafka

infra-down:
	docker compose down

backend:
	cd apps/backend && ./mvnw spring-boot:run

frontend:
	cd apps/frontend && npm run dev

api:
	./scripts/generate-api-types.sh

build:
	cd apps/backend && ./mvnw clean package
	cd apps/frontend && npm run build

test:
	cd apps/backend && ./mvnw test

lint:
	cd apps/frontend && npm run lint

clean:
	cd apps/backend && ./mvnw clean
	cd apps/frontend && rm -rf dist