-- ---------------------------
-- Table structure for pets
-- ---------------------------
CREATE TABLE IF NOT EXISTS pets (
  item_obj_id decimal(11) NOT NULL default 0,
  name varchar(16) ,
  level decimal(11) ,
  curHp decimal(18,0) ,
  curMp decimal(18,0) ,
  exp decimal(20) ,
  sp decimal(11) ,
  maxload decimal(11) ,
  fed decimal(11) ,
  weapon int(5) ,
  armor int(5) ,
  jewel int(5) ,
  PRIMARY KEY  (item_obj_id)
);
