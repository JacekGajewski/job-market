package com.jobmarket.scraper.cache;

import com.jobmarket.entity.ExperienceLevel;
import com.jobmarket.entity.MetricType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ScrapeSessionCache}.
 */
class ScrapeSessionCacheTest {

    private ScrapeSessionCache cache;

    @BeforeEach
    void setUp() {
        cache = new ScrapeSessionCache();
    }

    @Nested
    @DisplayName("put and get operations")
    class PutAndGetOperations {

        @Test
        @DisplayName("should store and retrieve a value")
        void shouldStoreAndRetrieveValue() {
            // given
            String key = "java|TOTAL|ALL|ALL|ANY";
            Integer value = 100;

            // when
            cache.put(key, value);
            Optional<Integer> result = cache.get(key);

            // then
            assertThat(result).isPresent().contains(100);
        }

        @Test
        @DisplayName("should return empty Optional for non-existent key")
        void shouldReturnEmptyForNonExistentKey() {
            // when
            Optional<Integer> result = cache.get("non-existent-key");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should overwrite existing value")
        void shouldOverwriteExistingValue() {
            // given
            String key = "java|TOTAL|ALL|ALL|ANY";
            cache.put(key, 100);

            // when
            cache.put(key, 200);
            Optional<Integer> result = cache.get(key);

            // then
            assertThat(result).isPresent().contains(200);
        }

        @Test
        @DisplayName("should store multiple different keys")
        void shouldStoreMultipleDifferentKeys() {
            // given
            String key1 = "java|TOTAL|ALL|ALL|ANY";
            String key2 = "python|WITH_SALARY|wroclaw|SENIOR|salaryCurrencyCode=pln";

            // when
            cache.put(key1, 100);
            cache.put(key2, 50);

            // then
            assertThat(cache.get(key1)).isPresent().contains(100);
            assertThat(cache.get(key2)).isPresent().contains(50);
        }
    }

    @Nested
    @DisplayName("clear operation")
    class ClearOperation {

        @Test
        @DisplayName("should remove all entries from cache")
        void shouldRemoveAllEntries() {
            // given
            cache.put("key1", 100);
            cache.put("key2", 200);
            cache.put("key3", 300);
            assertThat(cache.size()).isEqualTo(3);

            // when
            cache.clear();

            // then
            assertThat(cache.size()).isZero();
            assertThat(cache.get("key1")).isEmpty();
            assertThat(cache.get("key2")).isEmpty();
            assertThat(cache.get("key3")).isEmpty();
        }

        @Test
        @DisplayName("should be safe to clear empty cache")
        void shouldBeSafeToClearEmptyCache() {
            // given
            assertThat(cache.size()).isZero();

            // when/then - should not throw
            cache.clear();

            assertThat(cache.size()).isZero();
        }
    }

    @Nested
    @DisplayName("size operation")
    class SizeOperation {

        @Test
        @DisplayName("should return zero for empty cache")
        void shouldReturnZeroForEmptyCache() {
            assertThat(cache.size()).isZero();
        }

        @Test
        @DisplayName("should return correct count after additions")
        void shouldReturnCorrectCountAfterAdditions() {
            // when
            cache.put("key1", 100);
            cache.put("key2", 200);

            // then
            assertThat(cache.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("should not increase size on update of existing key")
        void shouldNotIncreaseSizeOnUpdate() {
            // given
            cache.put("key1", 100);
            assertThat(cache.size()).isEqualTo(1);

            // when
            cache.put("key1", 200);

            // then
            assertThat(cache.size()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("generateKey static method")
    class GenerateKeyMethod {

        @Test
        @DisplayName("should generate key with all parameters specified")
        void shouldGenerateKeyWithAllParams() {
            // when
            String key = ScrapeSessionCache.generateKey(
                    "java",
                    MetricType.WITH_SALARY,
                    "wroclaw",
                    ExperienceLevel.SENIOR,
                    "salaryCurrencyCode=pln&salaryMin=25000"
            );

            // then
            assertThat(key).isEqualTo("java|WITH_SALARY|wroclaw|SENIOR|salaryCurrencyCode=pln&salaryMin=25000");
        }

        @Test
        @DisplayName("should use ALL for null city")
        void shouldUseAllForNullCity() {
            // when
            String key = ScrapeSessionCache.generateKey(
                    "java",
                    MetricType.TOTAL,
                    null,
                    ExperienceLevel.MID,
                    null
            );

            // then
            assertThat(key).isEqualTo("java|TOTAL|ALL|MID|ANY");
        }

        @Test
        @DisplayName("should use ALL for null experience level")
        void shouldUseAllForNullExperienceLevel() {
            // when
            String key = ScrapeSessionCache.generateKey(
                    "python",
                    MetricType.REMOTE,
                    "slask",
                    null,
                    null
            );

            // then
            assertThat(key).isEqualTo("python|REMOTE|slask|ALL|ANY");
        }

        @Test
        @DisplayName("should use ANY for null salary params")
        void shouldUseAnyForNullSalaryParams() {
            // when
            String key = ScrapeSessionCache.generateKey(
                    "data",
                    MetricType.REMOTE_WITH_SALARY,
                    "wroclaw",
                    ExperienceLevel.JUNIOR,
                    null
            );

            // then
            assertThat(key).isEqualTo("data|REMOTE_WITH_SALARY|wroclaw|JUNIOR|ANY");
        }

        @Test
        @DisplayName("should generate key with all null optional params")
        void shouldGenerateKeyWithAllNullOptionalParams() {
            // when
            String key = ScrapeSessionCache.generateKey(
                    "java",
                    MetricType.TOTAL,
                    null,
                    null,
                    null
            );

            // then
            assertThat(key).isEqualTo("java|TOTAL|ALL|ALL|ANY");
        }

        @Test
        @DisplayName("should generate unique keys for different parameter combinations")
        void shouldGenerateUniqueKeysForDifferentCombinations() {
            // when
            String key1 = ScrapeSessionCache.generateKey("java", MetricType.TOTAL, null, null, null);
            String key2 = ScrapeSessionCache.generateKey("java", MetricType.WITH_SALARY, null, null, null);
            String key3 = ScrapeSessionCache.generateKey("java", MetricType.TOTAL, "wroclaw", null, null);
            String key4 = ScrapeSessionCache.generateKey("python", MetricType.TOTAL, null, null, null);

            // then
            assertThat(key1).isNotEqualTo(key2);
            assertThat(key1).isNotEqualTo(key3);
            assertThat(key1).isNotEqualTo(key4);
            assertThat(key2).isNotEqualTo(key3);
        }
    }
}
