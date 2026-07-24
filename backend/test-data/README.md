# Import Flow Test Data

This seed is deliberately separate from Flyway. It can be rerun without changing
the checksums of already-applied migrations.

## Seeded accounts

| Role | Username | Password |
|---|---|---|
| STAFF | `import_staff` | `Import@123` |
| MANAGER | `import_manager` | `Import@123` |

## Seeded inventory

| Resource | ID | Business key | Baseline |
|---|---:|---|---:|
| Warehouse | `99101` | `WH-IMPORT-TEST` | — |
| Product A | `99201` | `IMP-TEST-001` | `10` |
| Product B | `99202` | `IMP-TEST-002` | `0` |

The Postman flow imports 25 Product A units and 12 Product B units. The expected
final quantities are therefore 35 and 12.

## Load or reset the data

Start PostgreSQL first:

```powershell
cd backend
docker compose up -d postgres
Get-Content -Raw .\test-data\import-flow-seed.sql |
    docker exec -i training-postgres psql -v ON_ERROR_STOP=1 -U postgres -d training_db
```

Alternatively, with a local `psql` client:

```powershell
psql -v ON_ERROR_STOP=1 -h localhost -p 5433 -U postgres -d training_db `
    -f .\test-data\import-flow-seed.sql
```

Run the seed before starting the Postman flow. Rerunning it removes only
movements, stock, alerts, and reorder suggestions associated with
`WH-IMPORT-TEST` or `IMP-TEST-*`, then restores the baseline.

## Run the API flow

1. Start the full infrastructure with `docker compose up -d`.
2. Start the backend with `.\mvnw.cmd spring-boot:run`.
3. Import the collection and environment from the repository `postman/` folder.
4. Select the **StockPulse Local** environment.
5. Run the **Import Flow — Run in order** folder.

The collection logs in with both roles, verifies the baseline, creates an import,
approves it as MANAGER, completes it as STAFF, and verifies both final stock
quantities.
