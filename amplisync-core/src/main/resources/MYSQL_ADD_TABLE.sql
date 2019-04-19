DROP PROCEDURE IF EXISTS sqliteSyncCOM_AddTable;

KOL_SC

CREATE PROCEDURE sqliteSyncCOM_AddTable ()
BEGIN

DECLARE _count INT;
SET _count = (  SELECT COUNT(*)
				FROM INFORMATION_SCHEMA.COLUMNS
				WHERE   TABLE_SCHEMA=DATABASE() AND TABLE_NAME = '{$TABLE_NAME}' AND
						COLUMN_NAME = 'RowVer');
IF _count = 0 THEN
	ALTER TABLE {$TABLE_NAME}
		ADD COLUMN RowVer int DEFAULT 1;
END IF;

UPDATE {$TABLE_NAME} set RowVer=1 where RowVer is null;

SET _count = (  SELECT COUNT(*)
				FROM INFORMATION_SCHEMA.COLUMNS
				WHERE   TABLE_SCHEMA=DATABASE() AND TABLE_NAME = '{$TABLE_NAME}' AND
						COLUMN_NAME = 'RowId');
IF _count = 0 THEN
	ALTER TABLE {$TABLE_NAME}
		ADD COLUMN RowId char(36);
END IF;

UPDATE {$TABLE_NAME} set RowId=uuid() where RowId is null;

SET _count = (  SELECT COUNT(*)
				FROM INFORMATION_SCHEMA.COLUMNS
				WHERE   TABLE_SCHEMA=DATABASE() AND TABLE_NAME = 'MergeContent_{$TABLE_NAME}');
IF _count = 0 THEN

CREATE TABLE `MergeContent_{$TABLE_NAME}`(
	Id int NOT NULL AUTO_INCREMENT,
	TableId int NOT NULL,
	SubscriberId int NOT NULL,
	RowId CHAR(36) NOT NULL,
	RowVer int NOT NULL DEFAULT 1,
	ChangeDate timestamp NOT NULL DEFAULT NOW(),
	Action tinyint NOT NULL DEFAULT 1,
	SyncId int NULL,
	PRIMARY KEY (`Id`),
    INDEX `IX_1_{$SCHEMA_TABLE}_{$TABLE_NAME}` (`TableId` ASC, `SubscriberId` ASC,`RowId` ASC,`RowVer` ASC),
    INDEX `IX_{$SCHEMA_TABLE}_{$TABLE_NAME}` (`SyncId` ASC,`RowVer` ASC,`TableId` ASC,`SubscriberId` ASC),
    INDEX `IX_SubId_RowId_{$SCHEMA_TABLE}_{$TABLE_NAME}` (`SubscriberId` ASC,`RowId` ASC),
    INDEX `IX_SubscriberId_{$SCHEMA_TABLE}_{$TABLE_NAME}` (`SubscriberId` ASC),
    INDEX `IX_SubscriberId1_{$SCHEMA_TABLE}_{$TABLE_NAME}` (`TableId` ASC,`SubscriberId` ASC)
);

END IF;

SET _count = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE
	TABLE_CATALOG = 'def' AND TABLE_SCHEMA = DATABASE() AND
	TABLE_NAME = '{$TABLE_NAME}' AND INDEX_NAME = 'IX_MERGE_{$SCHEMA_TABLE}_{$TABLE_NAME}_RowId');
IF _count = 0 THEN

ALTER TABLE {$TABLE_NAME} ADD INDEX `IX_MERGE_{$SCHEMA_TABLE}_{$TABLE_NAME}_RowId` (`RowId` ASC);

END IF;

INSERT INTO MergeTablesToSync
           (TableName
		       ,TableSchema
           ,TableFilter
           ,ReadOnly
           ,IdentityRange
           ,IdentityTrashold)
     VALUES
           ('{$TABLE_NAME}'
		       ,''
           ,''
           ,0
           ,5000
           ,80);



END

KOL_SC

call sqliteSyncCOM_AddTable();

KOL_SC

DROP PROCEDURE IF EXISTS sqliteSyncCOM_AddTable;

KOL_SC

DROP TRIGGER IF EXISTS `prMerge_{$TABLE_NAME}Update`;

KOL_SC

CREATE TRIGGER `prMerge_{$TABLE_NAME}Update` BEFORE UPDATE ON `{$TABLE_NAME}` FOR EACH ROW
BEGIN
-- =============================================
-- Author:	SQLite-Sync.com Tomasz Dziemidowicz//All Right Reserved
-- =============================================

	SET NEW.RowVer = IFNULL(NEW.RowVer,1)+1;

END

KOL_SC

DROP TRIGGER IF EXISTS `prMerge_{$TABLE_NAME}Insert`;

KOL_SC

CREATE TRIGGER `prMerge_{$TABLE_NAME}Insert` BEFORE INSERT ON `{$TABLE_NAME}` FOR EACH ROW
BEGIN
-- =============================================
-- Author:	SQLite-Sync.com Tomasz Dziemidowicz//All Right Reserved
-- =============================================

	if (NEW.RowId is null) then
		SET NEW.RowId = uuid();
	end if;

END