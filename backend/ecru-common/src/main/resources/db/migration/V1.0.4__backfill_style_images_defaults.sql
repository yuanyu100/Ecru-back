UPDATE style_images
SET title = TRIM(style_category)
WHERE (title IS NULL OR TRIM(title) = '')
  AND style_category IS NOT NULL
  AND TRIM(style_category) <> '';

UPDATE style_images
SET source = '手工标注'
WHERE source IS NULL OR TRIM(source) = '';

UPDATE style_images
SET created_at = NOW()
WHERE created_at IS NULL;

UPDATE style_images
SET updated_at = COALESCE(updated_at, created_at, NOW())
WHERE updated_at IS NULL;
