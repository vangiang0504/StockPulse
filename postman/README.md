# StockPulse Postman Flows

## Import business flow

Files:

- `StockPulse-Import-Flow.postman_collection.json`
- `StockPulse-Local.postman_environment.json`
- Database seed: `../backend/test-data/import-flow-seed.sql`

Usage:

1. Execute `backend/test-data/import-flow-seed.sql` against `training_db`.
2. Import both Postman JSON files.
3. Select the **StockPulse Local** environment.
4. Start the backend and its required infrastructure.
5. Run the **Import Flow — Run in order** folder with the Collection Runner.

The nine requests automatically:

1. Log in as STAFF.
2. Log in as MANAGER.
3. Verify the database baseline.
4. Create an IMPORT movement as STAFF.
5. Read the pending movement.
6. Approve it as MANAGER.
7. Complete it as STAFF.
8. Verify both final stock quantities.
9. Verify the completed movement audit data.

Rerun the SQL seed before rerunning the collection so the stock baseline and
movement state are deterministic.
