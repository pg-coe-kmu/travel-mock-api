# Travel Mock API

A Spring Boot application that provides mock REST APIs for a travel booking system.

The application simulates hotel, flight, and car rental services by loading mock data from JSON files into memory during
startup. It is intended for development, testing, workflow automation, and AI agent integration without requiring access
to real booking providers.

## Features

- REST APIs for:
  - Hotels
  - Flights
  - Car rentals
- Mock data loaded from JSON files
- In-memory repositories
- Search endpoints with filtering
- Clean layered architecture
- Ready for integration with n8n and AI agents
- Designed to be replaceable with real provider APIs in the future

## Project Structure

```
src/main/java
├── config
├── constants
├── controller
├── loader
├── model
├── repository
├── service
└── TravelMockApiApplication.java

data
└── mock
    ├── airports.json
    ├── hotels.json
    ├── flights.json
    └── cars.json
```

## Architecture

```
HTTP Request
      │
      ▼
Controller
      │
      ▼
Service
      │
      ▼
Repository (In-Memory)
      ▲
      │
MockDataLoader
      ▲
      │
JSON Files
```

## Getting Started

### Requirements

- Java 21
- Maven 3.9+

### Run the application

```bash
git clone <repository-url>
cd travel-mock-api

mvn spring-boot:run
```

The application loads the mock data automatically during startup.

## Example Endpoints

### Search hotels

```http
GET /search/hotels?destination=Barcelona
```

### Search flights

```http
GET /search/flights?origin=Düsseldorf&destination=Barcelona&departureDate=2026-09-09
```

### Search rental cars

```http
GET /search/cars?location=Barcelona
```

### Get details

```http
GET /details/car?providerId=PROV-SIXT-01&carId=CAR-1001
GET /details/car?providerId=PROV-SIXT-01
GET /details/hotel?hotelId=HOT-1001&roomId=ROOM-101
GET /details/hotel?hotelId=HOT-1001
GET /details/flight?flightId=FL-1001
```

`carId`/`roomId` are optional: without them the full provider/hotel is returned; with them the provider/hotel is reduced to the matching car/room. Unknown ids return 404.

## Mock Data

The application loads mock data from:

```
data/mock/
├── airports.json
├── hotels.json
├── flights.json
└── cars.json
```

In future versions, the mock data can also be downloaded automatically from Cloudflare R2 during application startup.

## Technology Stack

- Java 21
- Spring Boot 3
- Maven
- Jackson
- Lombok
- SLF4J

## Roadmap

- [x] Hotel search API
- [x] Flight search API
- [ ] Car rental search API
- [x] JSON mock data loader
- [ ] Cloudflare R2 integration
- [ ] Booking endpoints
- [ ] Reservation workflow
- [ ] Payment simulation
- [ ] PDF invoice generation
- [ ] Booking confirmation endpoints
- [ ] Cancellation endpoints

## License

This project is intended for educational and development purposes.