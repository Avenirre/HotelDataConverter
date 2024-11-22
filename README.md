Hotel Data Converter

A Spring Boot application for processing hotel data from multiple sources (GIATA and COA) and downloading associated images.

## Task

Given is hotel data distributed in five xml files and one json file.
Write a converter, that creates one json document out of those files. All contents of the xml
files should be included in the one json file, but there should not be duplicates. Download
Images that are referenced inside the files (only images).

## Features

- Processes XML and JSON hotel data files
- Merges data from different sources for each hotel
- Extracts and downloads images from all data sources
- Validates image files integrity
- Provides REST API with OpenAPI documentation
- Supports asynchronous image downloading
- Generates a consolidated JSON output

## Limitations

- Maximum size per file: 10MB
- Maximum total size (6 files): 60MB

## Technologies

- JDK 21
- Spring Boot
- Maven
- Docker

## Request and response examples

Request:
```
curl -X POST http://localhost:8081/api/v1/converter \
-F "files=@C:\Users\aveni\Downloads\trial-task-java (1)\594608-coah.json" \
-F "files=@C:\Users\aveni\Downloads\trial-task-java (1)\411144-giata.xml" \
-F "files=@C:\Users\aveni\Downloads\trial-task-java (1)\162838-giata.xml" \
-F "files=@C:\Users\aveni\Downloads\trial-task-java (1)\162838-coah.xml" \
-F "files=@C:\Users\aveni\Downloads\trial-task-java (1)\3956-giata.xml" \
-F "files=@C:\Users\aveni\Downloads\trial-task-java (1)\3956-coah.xml"
```
Successful response example:
```
{
    "jsonFile": "C:\\Users\\...\\output\\20240121_123456\\hotels.json",
    "imagesDirectory": "C:\\Users\\...\\output\\20240121_123456\\images",
    "timestamp": "2024-01-21T12:34:56",
    "processedFiles": 6,
    "downloadedImages": 3 //number of successfully validated and downloaded images
}
```

Failed response example (for wrong file format):
```
{
    "message": "Invalid file type: wrongfile.txt",
    "status": 400,
    "error": "Bad Request",
    "timestamp": "2024-01-21 12:34:56"
}
```

## Swagger: 
http://localhost:8080/swagger-ui/index.html

## To build and run: 
docker-compose up --build
