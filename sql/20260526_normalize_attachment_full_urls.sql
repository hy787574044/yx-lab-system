-- Normalize stored attachment paths to full storage URLs.
-- Adjust this value before running in another environment.
SET @file_base_url = 'https://yangxin.yunhexx.com:8443';

SET @storage_prefix = CONCAT(TRIM(TRAILING '/' FROM @file_base_url), '/api/storage/file?path=');

UPDATE lab_user
SET avatar_url = CASE
    WHEN avatar_url IS NULL OR TRIM(avatar_url) = '' THEN avatar_url
    WHEN avatar_url REGEXP '^https?://.*/api/storage/file\\?path='
        THEN CONCAT(TRIM(TRAILING '/' FROM @file_base_url), SUBSTRING(avatar_url, LOCATE('/api/storage/file?path=', avatar_url)))
    WHEN avatar_url REGEXP '^https?://' OR avatar_url LIKE 'data:%' OR avatar_url LIKE 'blob:%'
        THEN avatar_url
    WHEN avatar_url LIKE '/api/storage/file?path=%'
        THEN CONCAT(TRIM(TRAILING '/' FROM @file_base_url), avatar_url)
    WHEN avatar_url LIKE 'api/storage/file?path=%'
        THEN CONCAT(TRIM(TRAILING '/' FROM @file_base_url), '/', avatar_url)
    ELSE CONCAT(@storage_prefix, TRIM(LEADING '/' FROM avatar_url))
END
WHERE avatar_url IS NOT NULL AND TRIM(avatar_url) <> '';

UPDATE lab_document
SET file_url = CASE
    WHEN file_url IS NULL OR TRIM(file_url) = '' THEN file_url
    WHEN file_url REGEXP '^https?://.*/api/storage/file\\?path='
        THEN CONCAT(TRIM(TRAILING '/' FROM @file_base_url), SUBSTRING(file_url, LOCATE('/api/storage/file?path=', file_url)))
    WHEN file_url REGEXP '^https?://' OR file_url LIKE 'data:%' OR file_url LIKE 'blob:%'
        THEN file_url
    WHEN file_url LIKE '/api/storage/file?path=%'
        THEN CONCAT(TRIM(TRAILING '/' FROM @file_base_url), file_url)
    WHEN file_url LIKE 'api/storage/file?path=%'
        THEN CONCAT(TRIM(TRAILING '/' FROM @file_base_url), '/', file_url)
    ELSE CONCAT(@storage_prefix, TRIM(LEADING '/' FROM file_url))
END
WHERE file_url IS NOT NULL AND TRIM(file_url) <> '';

UPDATE lab_instrument
SET certificate_url = CASE
    WHEN certificate_url IS NULL OR TRIM(certificate_url) = '' THEN certificate_url
    WHEN certificate_url REGEXP '^https?://.*/api/storage/file\\?path='
        THEN CONCAT(TRIM(TRAILING '/' FROM @file_base_url), SUBSTRING(certificate_url, LOCATE('/api/storage/file?path=', certificate_url)))
    WHEN certificate_url REGEXP '^https?://' OR certificate_url LIKE 'data:%' OR certificate_url LIKE 'blob:%'
        THEN certificate_url
    WHEN certificate_url LIKE '/api/storage/file?path=%'
        THEN CONCAT(TRIM(TRAILING '/' FROM @file_base_url), certificate_url)
    WHEN certificate_url LIKE 'api/storage/file?path=%'
        THEN CONCAT(TRIM(TRAILING '/' FROM @file_base_url), '/', certificate_url)
    ELSE CONCAT(@storage_prefix, TRIM(LEADING '/' FROM certificate_url))
END
WHERE certificate_url IS NOT NULL AND TRIM(certificate_url) <> '';

UPDATE lab_report
SET file_path = CASE
    WHEN file_path IS NULL OR TRIM(file_path) = '' THEN file_path
    WHEN file_path REGEXP '^https?://.*/api/storage/file\\?path='
        THEN CONCAT(TRIM(TRAILING '/' FROM @file_base_url), SUBSTRING(file_path, LOCATE('/api/storage/file?path=', file_path)))
    WHEN file_path REGEXP '^https?://' OR file_path LIKE 'data:%' OR file_path LIKE 'blob:%'
        THEN file_path
    WHEN file_path LIKE '/api/storage/file?path=%'
        THEN CONCAT(TRIM(TRAILING '/' FROM @file_base_url), file_path)
    WHEN file_path LIKE 'api/storage/file?path=%'
        THEN CONCAT(TRIM(TRAILING '/' FROM @file_base_url), '/', file_path)
    ELSE CONCAT(@storage_prefix, TRIM(LEADING '/' FROM file_path))
END
WHERE file_path IS NOT NULL AND TRIM(file_path) <> '';

DROP TEMPORARY TABLE IF EXISTS tmp_sampling_task_photo_url_parts;
CREATE TEMPORARY TABLE tmp_sampling_task_photo_url_parts (
    task_id BIGINT NOT NULL,
    part_index INT NOT NULL,
    normalized_url TEXT NOT NULL
);

INSERT INTO tmp_sampling_task_photo_url_parts (task_id, part_index, normalized_url)
WITH RECURSIVE split_photo_urls AS (
    SELECT
        id AS task_id,
        1 AS part_index,
        TRIM(SUBSTRING_INDEX(photo_urls, ',', 1)) AS part_value,
        CASE
            WHEN LOCATE(',', photo_urls) > 0 THEN SUBSTRING(photo_urls, LOCATE(',', photo_urls) + 1)
            ELSE ''
        END AS rest_value
    FROM lab_sampling_task
    WHERE photo_urls IS NOT NULL AND TRIM(photo_urls) <> ''
    UNION ALL
    SELECT
        task_id,
        part_index + 1,
        TRIM(SUBSTRING_INDEX(rest_value, ',', 1)),
        CASE
            WHEN LOCATE(',', rest_value) > 0 THEN SUBSTRING(rest_value, LOCATE(',', rest_value) + 1)
            ELSE ''
        END
    FROM split_photo_urls
    WHERE rest_value <> ''
)
SELECT
    task_id,
    part_index,
    CASE
        WHEN part_value IS NULL OR TRIM(part_value) = '' THEN part_value
        WHEN part_value REGEXP '^https?://.*/api/storage/file\\?path='
            THEN CONCAT(TRIM(TRAILING '/' FROM @file_base_url), SUBSTRING(part_value, LOCATE('/api/storage/file?path=', part_value)))
        WHEN part_value REGEXP '^https?://' OR part_value LIKE 'data:%' OR part_value LIKE 'blob:%'
            THEN part_value
        WHEN part_value LIKE '/api/storage/file?path=%'
            THEN CONCAT(TRIM(TRAILING '/' FROM @file_base_url), part_value)
        WHEN part_value LIKE 'api/storage/file?path=%'
            THEN CONCAT(TRIM(TRAILING '/' FROM @file_base_url), '/', part_value)
        ELSE CONCAT(@storage_prefix, TRIM(LEADING '/' FROM part_value))
    END AS normalized_url
FROM split_photo_urls
WHERE part_value IS NOT NULL AND TRIM(part_value) <> '';

UPDATE lab_sampling_task task
JOIN (
    SELECT task_id, GROUP_CONCAT(normalized_url ORDER BY part_index SEPARATOR ',') AS photo_urls
    FROM tmp_sampling_task_photo_url_parts
    GROUP BY task_id
) normalized ON normalized.task_id = task.id
SET task.photo_urls = normalized.photo_urls;

DROP TEMPORARY TABLE IF EXISTS tmp_sampling_task_photo_url_parts;
