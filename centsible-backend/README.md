# Centsible — Backend 

Personal finance dashboard backend. This sets up the data model (Category, Transaction),
an H2 file-based database, and seeds it with ~30 realistic sample transactions across
2 months so there's real data to build against before CSV upload exists.

## Requirements
- Java 17+
- Maven 3.6+

## Run it
```bash
cd centsible-backend
mvn spring-boot:run
```

On first run, `DataSeeder` will populate the database automatically. On later runs it
detects existing data and skips reseeding (so restarting the app is safe).

## Verify the data
Open the H2 web console: http://localhost:8080/h2-console

Login with:
- JDBC URL: `jdbc:h2:file:./data/centsible`
- User Name: `sa`
- Password: *(leave blank)*

Then run e.g. `SELECT * FROM TRANSACTIONS;` to see the seeded rows.

## What's in this step
- `model/Category.java`, `model/Transaction.java` — JPA entities (the SQL schema is
  auto-generated from these via Hibernate)
- `repository/` — Spring Data repositories for querying
- `config/DataSeeder.java` — seeds sample data on startup
- No REST endpoints yet — that's step 2 (aggregation endpoints + first chart)

## Note on this container
This project was scaffolded here but not build-tested in this sandbox — the sandbox
can't reach Maven Central to download Spring Boot dependencies. Run `mvn spring-boot:run`
locally in your own environment (IntelliJ, VS Code, or terminal) to actually build and
start it. If you hit any errors when you run it, paste them back here and we'll fix them.
