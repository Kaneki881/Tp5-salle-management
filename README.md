# TP5 - Salle Management

A small Java project that finds available meeting rooms with JPA/Hibernate on an in-memory H2 database. It is organized in layers (service, repository, model) and shows how to build a dynamic search with the Criteria API, an availability check with a subquery, and pagination.

## Features

- List the rooms that are free on a given time slot
- Search rooms with optional filters: part of the name, minimum capacity, and availability on a time slot
- Paginated results with the total number of matches and the number of pages
- Input validation in the service layer (time slot, page number, page size, capacity)

## Technologies

- Java (8 or newer)
- Maven
- Hibernate Core 5.4.30.Final (JPA 2.2 implementation)
- JPA API 2.2 (`javax.persistence`)
- H2 Database 1.4.200 (in-memory, runtime scope)

## Project structure

```
Tp5-salle-management
├── pom.xml
└── src
    ├── main
    │   ├── java/com/example
    │   │   ├── App.java                          # Demo: sample data, availability, paginated search
    │   │   ├── model
    │   │   │   ├── Salle.java                    # A room
    │   │   │   └── Reservation.java              # A booking of a room on a time slot
    │   │   ├── repository
    │   │   │   ├── SalleRepository.java          # Query interface
    │   │   │   └── SalleRepositoryImpl.java      # JPQL and Criteria API queries
    │   │   ├── service
    │   │   │   ├── SalleService.java             # Business interface
    │   │   │   └── SalleServiceImpl.java         # Validation, then calls the repository
    │   │   └── util
    │   │       └── PaginationResult.java         # One page of results with its counters
    │   └── resources/META-INF
    │       └── persistence.xml                   # Persistence unit "SallePU"
    └── test/java/com/example
        └── AppTest.java                          # Placeholder test
```

## Architecture

```
App  ──>  SalleService  ──>  SalleRepository  ──>  EntityManager (H2)
          (validation)       (queries)
```

- `SalleServiceImpl` checks the arguments and throws an `IllegalArgumentException` if they are wrong, then delegates to the repository.
- `SalleRepositoryImpl` builds and runs the queries. It receives an `EntityManager` in its constructor and never opens or closes it: the caller does.
- `PaginationResult<T>` is an immutable holder for one page.

## Data model

### Salle (table `salles`)

| Field | Type | Constraints |
|---|---|---|
| `id` | `Long` | Generated primary key (`IDENTITY`) |
| `nom` | `String` | Required, unique |
| `capacite` | `int` | Required |
| `reservations` | `Set<Reservation>` | One-to-many, lazy, `cascade = ALL`, inverse side |

### Reservation (table `reservations`)

| Field | Type | Constraints |
|---|---|---|
| `id` | `Long` | Generated primary key (`IDENTITY`) |
| `salle` | `Salle` | Required, many-to-one, lazy, column `salle_id` |
| `debut` | `LocalDateTime` | Required |
| `fin` | `LocalDateTime` | Required |

## Availability rule

A room is available on a slot `[debut, fin[` if it has no reservation that overlaps it. A reservation overlaps the slot when:

```
reservation.debut < fin   AND   reservation.fin > debut
```

The comparisons are strict, so back-to-back slots do not conflict: a room booked from 09:00 to 10:00 is still available from 10:00 to 11:00.

The check is a `NOT EXISTS` subquery in `findAvailableRooms` (JPQL) and in the search filter (Criteria API).

## Search and pagination

`SalleService.searchRooms(nom, capaciteMin, debut, fin, page, size)` combines optional filters. A filter that is `null` is ignored, and all active filters are combined with `AND`.

| Parameter | Effect |
|---|---|
| `nom` | The room name contains this text, ignoring case. Blank values are ignored |
| `capaciteMin` | The capacity is at least this value |
| `debut`, `fin` | The room has no reservation overlapping this slot. Both must be given, or neither |
| `page` | Page number, starting at 0 |
| `size` | Number of rooms per page |

Results are sorted by name, then by id. The service runs a count query and a page query, and returns a `PaginationResult` with `getItems()`, `getCurrentPage()`, `getPageSize()`, `getTotalItems()` and `getTotalPages()`.

### Validation in the service

An `IllegalArgumentException` is thrown when:

- `page` is negative
- `size` is less than 1
- `capaciteMin` is negative
- only one of `debut` and `fin` is given
- `debut` or `fin` is missing, or `debut` is not strictly before `fin` (for `getAvailableRooms`, and for `searchRooms` when a slot is given)

The repository also rejects a page offset that does not fit in an `int`.

## What the demo does

`App.main` creates the `EntityManagerFactory` for the unit `SallePU`, then:

1. Saves three rooms and two reservations, all on 2026-10-11:

   | Room | Capacity | Reservation |
   |---|---|---|
   | Alpha | 20 | 09:00 to 10:00 |
   | Beta | 30 | 10:00 to 11:00 |
   | Gamma | 8 | none |

2. Lists the rooms available from 10:00 to 11:00
3. Searches rooms with a capacity of at least 10, available on the same slot, with one room per page, and prints the total, the number of pages and the first page

Alpha is available because its reservation ends exactly when the slot starts. Beta is not, and Gamma is too small for the search. The console output, apart from the SQL logged by Hibernate, should be:

```
Salles disponibles :
- Alpha (capacité : 20)
- Gamma (capacité : 8)
Total filtré : 1
Nombre de pages : 1
Page 0 : Alpha
```

## Configuration

The persistence unit is defined in `src/main/resources/META-INF/persistence.xml`:

- H2 in-memory database `jdbc:h2:mem:salles`, user `sa`, empty password
- `javax.persistence.schema-generation.database.action=drop-and-create`: tables are dropped and created at startup
- `hibernate.show_sql=true`: generated SQL is printed in the console
- Only `Salle` and `Reservation` are mapped (`exclude-unlisted-classes`)

The database lives in memory, so all data is lost when the program ends.

## Build and run

```bash
mvn compile exec:java -Dexec.mainClass=com.example.App
```
## Results
<img width="1847" height="943" alt="11-10" src="https://github.com/user-attachments/assets/5d673096-c6de-402e-84e5-b1cd56e36c16" />

## Notes

- **Tests:** `AppTest.java` uses JUnit, which is not declared in `pom.xml`. Add the `junit` dependency (test scope) before running `mvn test` or `mvn package`.
- **Java version:** `pom.xml` does not set a compiler level. Add `maven.compiler.source` and `maven.compiler.target` (for example `11`) to `<properties>` to be sure it builds on every Maven setup.
- **Name filter:** the text typed in `nom` is used in a `LIKE` as is, so `%` and `_` act as wildcards instead of plain characters.
- **`Salle.reservations`:** the set is not initialized, so it is `null` on a new `Salle`. Initialize it with `new HashSet<>()` before adding reservations from the room side.
- **Bookings:** `Reservation` has no date validation and nothing prevents two overlapping bookings. The project only searches existing data.

## Possible next steps

- Add a `reserver` operation in the service that refuses overlapping bookings and checks that the start is before the end
- Sort the search by another field, chosen by the caller
- Add real JUnit tests for the availability rule and the pagination, including back-to-back slots and an empty result
