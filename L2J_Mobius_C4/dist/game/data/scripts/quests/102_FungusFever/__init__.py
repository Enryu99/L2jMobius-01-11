# Made by Mr. Have fun! Version 0.2
import sys
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

ALBERRYUS_LETTER_ID = 964
EVERGREEN_AMULET_ID = 965
DRYAD_TEARS_ID = 966
ALBERRYUS_LIST_ID = 746
COBS_MEDICINE1_ID = 1130
COBS_MEDICINE2_ID = 1131
COBS_MEDICINE3_ID = 1132
COBS_MEDICINE4_ID = 1133
COBS_MEDICINE5_ID = 1134
SWORD_OF_SENTINEL_ID = 743
STAFF_OF_SENTINEL_ID = 744

def check(st) :
   if (st.getQuestItemsCount(COBS_MEDICINE1_ID)==\
       st.getQuestItemsCount(COBS_MEDICINE2_ID)==\
       st.getQuestItemsCount(COBS_MEDICINE3_ID)==\
       st.getQuestItemsCount(COBS_MEDICINE4_ID)==\
       st.getQuestItemsCount(COBS_MEDICINE5_ID)==0) :
       st.set("cond","6")
       st.playSound("ItemSound.quest_middle")

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [ALBERRYUS_LETTER_ID, EVERGREEN_AMULET_ID, DRYAD_TEARS_ID, COBS_MEDICINE1_ID, COBS_MEDICINE2_ID, COBS_MEDICINE3_ID, COBS_MEDICINE4_ID, COBS_MEDICINE5_ID, ALBERRYUS_LIST_ID]

 def onEvent (self,event,st) :
    htmltext = event
    if event == "1" :
        htmltext = "7284-02.htm"
        st.giveItems(ALBERRYUS_LETTER_ID,1)
        st.set("cond","1")
        st.setState(STARTED)
        st.playSound("ItemSound.quest_accept")
    return htmltext

 def onTalk (Self,npc,st):
   npcId = npc.getNpcId()
   htmltext = "<html><body>I have nothing to say to you.</body></html>"
   id = st.getState()
   if id == CREATED :
     st.set("cond","0")
     st.set("onlyone","0")
   if npcId == 7284 and st.getInt("cond")==0 and st.getInt("onlyone")==0 :
      if st.getPlayer().getRace().ordinal() != 1 :
         htmltext = "7284-00.htm"
         st.exitQuest(1)
      elif st.getPlayer().getLevel() >= 12 :
         htmltext = "7284-07.htm"
         return htmltext
      else:
         htmltext = "7284-08.htm"
         st.exitQuest(1)
   elif npcId == 7284 and st.getInt("cond")==0 and st.getInt("onlyone")==1 :
        htmltext = "<html><body>This quest has already been completed.</body></html>"
   elif npcId == 7284 and st.getInt("cond")==1 and st.getQuestItemsCount(ALBERRYUS_LETTER_ID)==1 :
        htmltext = "7284-03.htm"
   elif npcId == 7284 and st.getInt("cond")==1 and st.getQuestItemsCount(EVERGREEN_AMULET_ID)==1 :
        htmltext = "7284-09.htm"
   elif npcId == 7156 and st.getInt("cond")==1 and st.getQuestItemsCount(ALBERRYUS_LETTER_ID)==1 :
        st.giveItems(EVERGREEN_AMULET_ID,1)
        st.takeItems(ALBERRYUS_LETTER_ID,1)
        st.set("cond","2")
        htmltext = "7156-03.htm"
   elif npcId == 7156 and st.getInt("cond")==2 and st.getQuestItemsCount(EVERGREEN_AMULET_ID)>0 and st.getQuestItemsCount(DRYAD_TEARS_ID)<10 :
        htmltext = "7156-04.htm"
   elif npcId == 7156 and st.getInt("cond")==5 and st.getQuestItemsCount(ALBERRYUS_LIST_ID)>0 :
        htmltext = "7156-07.htm"
   elif npcId == 7156 and st.getInt("cond")==3 and st.getQuestItemsCount(EVERGREEN_AMULET_ID)>0 and st.getQuestItemsCount(DRYAD_TEARS_ID)>=10 :
        st.takeItems(EVERGREEN_AMULET_ID,1)
        st.takeItems(DRYAD_TEARS_ID,-1)
        st.giveItems(COBS_MEDICINE1_ID,1)
        st.giveItems(COBS_MEDICINE2_ID,1)
        st.giveItems(COBS_MEDICINE3_ID,1)
        st.giveItems(COBS_MEDICINE4_ID,1)
        st.giveItems(COBS_MEDICINE5_ID,1)
        st.set("cond","4")
        htmltext = "7156-05.htm"
   elif npcId == 7156 and st.getInt("cond")==4 and st.getQuestItemsCount(ALBERRYUS_LIST_ID)==0 and (st.getQuestItemsCount(COBS_MEDICINE1_ID)==1 or st.getQuestItemsCount(COBS_MEDICINE2_ID)==1 or st.getQuestItemsCount(COBS_MEDICINE3_ID)==1 or st.getQuestItemsCount(COBS_MEDICINE4_ID)==1 or st.getQuestItemsCount(COBS_MEDICINE5_ID)==1) :
        htmltext = "7156-06.htm"
   elif npcId == 7284 and st.getInt("cond")==4 and st.getQuestItemsCount(ALBERRYUS_LIST_ID)==0 and st.getQuestItemsCount(COBS_MEDICINE1_ID)==1 :
        st.takeItems(COBS_MEDICINE1_ID,1)
        st.giveItems(ALBERRYUS_LIST_ID,1)
        st.set("cond","5")
        htmltext = "7284-04.htm"
   elif npcId == 7284 and st.getInt("cond")==5 and st.getQuestItemsCount(ALBERRYUS_LIST_ID)==1 and (st.getQuestItemsCount(COBS_MEDICINE1_ID)==1 or st.getQuestItemsCount(COBS_MEDICINE2_ID)==1 or st.getQuestItemsCount(COBS_MEDICINE3_ID)==1 or st.getQuestItemsCount(COBS_MEDICINE4_ID)==1 or st.getQuestItemsCount(COBS_MEDICINE5_ID)==1) :
        htmltext = "7284-05.htm"
   elif npcId == 7217 and st.getInt("cond")==5 and st.getQuestItemsCount(ALBERRYUS_LIST_ID)==1 and st.getQuestItemsCount(COBS_MEDICINE2_ID)==1 :
        st.takeItems(COBS_MEDICINE2_ID,1)
        check(st)
        htmltext = "7217-01.htm"
   elif npcId == 7219 and st.getInt("cond")==5 and st.getQuestItemsCount(ALBERRYUS_LIST_ID)==1 and st.getQuestItemsCount(COBS_MEDICINE3_ID)==1 :
        st.takeItems(COBS_MEDICINE3_ID,1)
        check(st)
        htmltext = "7219-01.htm"
   elif npcId == 7221 and st.getInt("cond")==5 and st.getQuestItemsCount(ALBERRYUS_LIST_ID)==1 and st.getQuestItemsCount(COBS_MEDICINE4_ID)==1 :
        st.takeItems(COBS_MEDICINE4_ID,1)
        check(st)
        htmltext = "7221-01.htm"
   elif npcId == 7285 and st.getInt("cond")==5 and st.getQuestItemsCount(ALBERRYUS_LIST_ID)==1 and st.getQuestItemsCount(COBS_MEDICINE5_ID)==1 :
        st.takeItems(COBS_MEDICINE5_ID,1)
        check(st)
        htmltext = "7285-01.htm"
   elif npcId == 7284 and st.getInt("cond")==6 and st.getQuestItemsCount(ALBERRYUS_LIST_ID)==1 :
        st.takeItems(ALBERRYUS_LIST_ID,1)
        st.set("cond","0")
        st.setState(COMPLETED)
        st.playSound("ItemSound.quest_finish")
        htmltext = "7284-06.htm"
        st.set("onlyone","1")
        if st.getPlayer().getClassId().getId() in range(18,25) :
          st.giveItems(SWORD_OF_SENTINEL_ID,1)
        else:
          st.giveItems(STAFF_OF_SENTINEL_ID,1)
   return htmltext

 def onKill (self,npc,player,isPet):
   st = player.getQuestState("102_FungusFever")
   if st:
      if st.getState() != STARTED : return
      npcId = npc.getNpcId()
      if npcId in [13,19] :
         if st.getQuestItemsCount(EVERGREEN_AMULET_ID)>0 and st.getQuestItemsCount(DRYAD_TEARS_ID)<10 :
            if st.getRandom(10)<3 :
               st.giveItems(DRYAD_TEARS_ID,1)
               if st.getQuestItemsCount(DRYAD_TEARS_ID) == 10 :
                 st.playSound("ItemSound.quest_middle")
                 st.set("cond","3")
               else:
                 st.playSound("ItemSound.quest_itemget")
   return

QUEST       = Quest(102,"102_FungusFever","Fungus Fever")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(7284)
QUEST.addTalkId(7284)

QUEST.addTalkId(7156)
QUEST.addTalkId(7217)
QUEST.addTalkId(7219)
QUEST.addTalkId(7221)
QUEST.addTalkId(7284)
QUEST.addTalkId(7285)

QUEST.addKillId(13)
QUEST.addKillId(19)