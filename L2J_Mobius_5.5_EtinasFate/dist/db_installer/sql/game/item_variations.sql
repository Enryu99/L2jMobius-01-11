CREATE TABLE IF NOT EXISTS `item_variations` (
  `itemId` INT(11) NOT NULL,
  `mineralId` INT(11) NOT NULL DEFAULT 0,
  `option1` INT(11) NOT NULL,
  `option2` INT(11) NOT NULL,
  PRIMARY KEY (`itemId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;