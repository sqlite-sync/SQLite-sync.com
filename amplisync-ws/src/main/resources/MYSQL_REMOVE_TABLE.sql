drop table `MergeContent_{$TABLE_NAME}`;

KOL_SC

DROP TRIGGER IF EXISTS `prMerge_{$TABLE_NAME}Update`;

KOL_SC

DROP TRIGGER IF EXISTS `prMerge_{$TABLE_NAME}Insert`;

KOL_SC

ALTER TABLE `{$TABLE_NAME}`
DROP COLUMN `RowId`,
DROP COLUMN `RowVer`,
DROP INDEX `IX_MERGE_{$SCHEMA_TABLE}_{$TABLE_NAME}_RowId`;

KOL_SC

delete from MergeTablesToSync where TableName='{$TABLE_NAME}';