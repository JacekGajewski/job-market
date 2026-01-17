# REST API Implementation Plan

## Overview
REST API layer exposing category management and statistics endpoints.

## 1. API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/categories` | List all tracked categories |
| POST | `/api/categories` | Add new category |
| DELETE | `/api/categories/{id}` | Remove category |
| GET | `/api/stats/{category}` | Historical data (with date range) |
| GET | `/api/stats/{category}/latest` | Most recent count |

## 2. Package Structure

```
com.jobmarket/
├── controller/
│   ├── CategoryController.java
│   └── StatsController.java
├── dto/
│   ├── CategoryDto.java
│   ├── CreateCategoryRequest.java
│   ├── JobCountStatsDto.java
│   ├── LatestCountDto.java
│   └── ErrorResponse.java
├── service/
│   ├── CategoryService.java
│   └── StatsService.java
├── mapper/
│   ├── CategoryMapper.java
│   └── JobCountMapper.java
├── exception/
│   ├── CategoryNotFoundException.java
│   ├── DuplicateCategoryException.java
│   └── GlobalExceptionHandler.java
└── config/
    ├── WebConfig.java
    └── OpenApiConfig.java
```

## 3. DTOs

### CategoryDto.java
```java
@Builder
public record CategoryDto(
    Long id,
    String name,
    String slug,
    boolean active
) {}
```

### CreateCategoryRequest.java
```java
public record CreateCategoryRequest(
    @NotBlank @Size(min = 2, max = 50)
    String name,

    @NotBlank @Pattern(regexp = "^[a-z0-9-]+$")
    String slug
) {}
```

### JobCountStatsDto.java
```java
@Builder
public record JobCountStatsDto(
    Long id,
    String category,
    Integer count,
    LocalDateTime fetchedAt,
    String location
) {}
```

### LatestCountDto.java
```java
@Builder
public record LatestCountDto(
    String category,
    Integer count,
    LocalDateTime fetchedAt,
    Integer changeFromPrevious,
    Double percentageChange
) {}
```

## 4. Controllers

### CategoryController.java
```java
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAllCategories() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(categoryService.create(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### StatsController.java
```java
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {
    private final StatsService statsService;

    @GetMapping("/{category}")
    public ResponseEntity<List<JobCountStatsDto>> getStats(
            @PathVariable String category,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(statsService.getHistoricalData(category, startDate, endDate));
    }

    @GetMapping("/{category}/latest")
    public ResponseEntity<LatestCountDto> getLatestCount(@PathVariable String category) {
        return ResponseEntity.ok(statsService.getLatestCount(category));
    }
}
```

## 5. Exception Handling

### GlobalExceptionHandler.java
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(CategoryNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateCategoryException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateCategoryException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> new FieldError(e.getField(), e.getDefaultMessage(), e.getRejectedValue()))
            .toList();
        // return with fieldErrors
    }
}
```

## 6. CORS Configuration

### WebConfig.java
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(allowedOrigins)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

## 7. OpenAPI/Swagger

### Dependencies
```groovy
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
```

### Access
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## 8. Sample Responses

### GET /api/categories
```json
[
  {"id": 1, "name": "Java", "slug": "java", "active": true},
  {"id": 2, "name": "Python", "slug": "python", "active": true}
]
```

### GET /api/stats/java/latest
```json
{
  "category": "java",
  "count": 1312,
  "fetchedAt": "2024-01-15T08:00:00",
  "changeFromPrevious": 12,
  "percentageChange": 0.92
}
```

### Error Response
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Category not found with slug: golang",
  "path": "/api/stats/golang/latest"
}
```

## 9. Implementation Checklist

- [ ] Create all DTO classes
- [ ] Create custom exceptions
- [ ] Implement GlobalExceptionHandler
- [ ] Create mapper classes
- [ ] Implement CategoryService
- [ ] Implement StatsService
- [ ] Create CategoryController
- [ ] Create StatsController
- [ ] Configure CORS
- [ ] Add OpenAPI configuration
- [ ] Write controller tests
