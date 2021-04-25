-- ----------------------------------------------------------------------------
-- Table dbo.MergeIdentity
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `MergeIdentity` (
  `Id` INT NOT NULL AUTO_INCREMENT,
  `TableId` INT NOT NULL,
  `SubscriberId` INT NOT NULL,
  `Rev` INT NOT NULL DEFAULT 1,
  `IdentityStart` INT NOT NULL,
  `IdentityEnd` INT NOT NULL,
  `IdentityCurrent` INT NOT NULL,
  `RowId` VARCHAR(64) UNIQUE NOT NULL,
  `MergeInsertSource` TINYINT UNSIGNED NULL DEFAULT 1,
  `RowVer` INT NOT NULL DEFAULT 1,
  PRIMARY KEY (`Id`),
  INDEX `IX_MergeIdentity` (`SubscriberId` ASC, `TableId` ASC));

KOL_SC

CREATE TABLE `MergeContent_MergeIdentity` (
  `Id` int NOT NULL AUTO_INCREMENT,
  `TableId` int NOT NULL,
  `SubscriberId` int NOT NULL,
  `RowId` char(36) NOT NULL,
  `RowVer` int NOT NULL DEFAULT '1',
  `ChangeDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `Action` tinyint NOT NULL DEFAULT '1',
  `SyncId` int DEFAULT NULL,
  PRIMARY KEY (`Id`),
  KEY `IX_MergeContent_1_MergeIdentity` (`TableId`,`SubscriberId`,`RowId`,`RowVer`),
  KEY `IX_MergeContent_MergeIdentity` (`SyncId`,`RowVer`,`TableId`,`SubscriberId`),
  KEY `IX_MergeContent_SubId_RowId_MergeIdentity` (`SubscriberId`,`RowId`),
  KEY `IX_MergeContent_SubscriberId_MergeIdentity` (`SubscriberId`),
  KEY `IX_MergeContent_SubscriberId1_MergeIdentity` (`TableId`,`SubscriberId`)
);

KOL_SC

CREATE TRIGGER `prMerge_MergeIdentityUpdate` BEFORE UPDATE ON `MergeIdentity` FOR EACH ROW
BEGIN
-- =============================================
-- Author:	SQLite-Sync.com Tomasz Dziemidowicz//All Right Reserved
-- =============================================

	SET NEW.RowVer := IFNULL(NEW.RowVer,1)+1;

END;

KOL_SC

CREATE TRIGGER `prMerge_MergeIdentityInsert` BEFORE INSERT ON `MergeIdentity` FOR EACH ROW
BEGIN
-- =============================================
-- Author:	SQLite-Sync.com Tomasz Dziemidowicz//All Right Reserved
-- =============================================

	SET NEW.RowId := uuid();

END;

KOL_SC

-- ----------------------------------------------------------------------------
-- Table dbo.MergeSubscribers
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `MergeSubscribers` (
  `SubscriberId` INT NOT NULL AUTO_INCREMENT,
  `Name` VARCHAR(200) NOT NULL,
  `UniqueName` VARCHAR(100) NOT NULL,
  `NeedReinitialization` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`SubscriberId`),
  UNIQUE INDEX `IX_MergeSubscribers` (`UniqueName` ASC));

KOL_SC

-- ----------------------------------------------------------------------------
-- Table dbo.MergeSync
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `MergeSync` (
  `SyncId` INT NOT NULL AUTO_INCREMENT,
  `SubscriberId` INT NOT NULL,
  `SyncStart` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `SyncEnd` DATETIME NULL,
  `SyncObject` LONGTEXT NULL,
  `TableId` INT NULL,
  PRIMARY KEY (`SyncId`));

KOL_SC

-- ----------------------------------------------------------------------------
-- Table dbo.MergeTablesToSync
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `MergeTablesToSync` (
  `TableId` INT NOT NULL AUTO_INCREMENT,
  `TableName` VARCHAR(510) NULL,
  `TableSchema` VARCHAR(510) NULL,
  `TableFilter` VARCHAR(4000) NULL,
  `ReadOnly` TINYINT(1) NOT NULL DEFAULT 0,
  `IdentityRange` INT NULL DEFAULT 2000,
  `IdentityTrashold` TINYINT UNSIGNED NULL DEFAULT 80,
  PRIMARY KEY (`TableId`),
  INDEX `IX_MergeTablesToSync` (`TableName`(255) ASC));