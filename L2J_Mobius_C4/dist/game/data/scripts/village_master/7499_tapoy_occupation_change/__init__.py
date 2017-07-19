#
# Created by DraX on 2005.08.15
#

import sys

from com.l2jmobius.gameserver.model.quest        import State
from com.l2jmobius.gameserver.model.quest        import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

PASS_FINAL_ID         = 1635
HEAD_BLACKSMITH_TAPOY = 7499

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onEvent (self,event,st):
   htmltext = "No Quest"
   Race     = st.getPlayer().getRace()
   ClassId  = st.getPlayer().getClassId()
   Level    = st.getPlayer().getLevel()
   if event == "7499-01.htm":
     return "7499-01.htm"
   if event == "7499-02.htm":
     return "7499-02.htm"
   if event == "7499-03.htm":
     return "7499-03.htm"
   if event == "7499-04.htm":
     return "7499-04.htm"
   if event == "class_change_56":
     if ClassId in [ClassId.dwarvenFighter]:
        if Level <= 19 and st.getQuestItemsCount(PASS_FINAL_ID) == 0:
          htmltext = "7499-05.htm"
        if Level <= 19 and st.getQuestItemsCount(PASS_FINAL_ID) >= 1:
          htmltext = "7499-06.htm"
        if Level >= 20 and st.getQuestItemsCount(PASS_FINAL_ID) == 0:
          htmltext = "7499-07.htm"
        if Level >= 20 and st.getQuestItemsCount(PASS_FINAL_ID) >= 1:
          st.takeItems(PASS_FINAL_ID,1)
          st.getPlayer().setClassId(56)
          st.getPlayer().setBaseClass(56)
          st.getPlayer().broadcastUserInfo()
          st.playSound("ItemSound.quest_fanfare_2")
          htmltext = "7499-08.htm"
   st.setState(COMPLETED)
   st.exitQuest(1)
   return htmltext

 def onTalk (Self,npc,st):
   npcId = npc.getNpcId()
   Race    = st.getPlayer().getRace()
   ClassId = st.getPlayer().getClassId()
   # Dwarfs got accepted
   if npcId == HEAD_BLACKSMITH_TAPOY and Race in [Race.Dwarf]:
     if ClassId in [ClassId.dwarvenFighter]:
       st.setState(STARTED)
       return "7499-01.htm"
     if ClassId in [ClassId.scavenger, ClassId.artisan]:
       st.setState(COMPLETED)
       st.exitQuest(1)
       return "7499-09.htm"
     if ClassId in [ClassId.bountyHunter, ClassId.warsmith]:
       st.setState(COMPLETED)
       st.exitQuest(1)
       return "7499-10.htm"
   # All other Races must be out
   if npcId == HEAD_BLACKSMITH_TAPOY and Race in [Race.Elf, Race.DarkElf, Race.Orc, Race.Human]:
     st.setState(COMPLETED)
     st.exitQuest(1)
     return "7499-11.htm"

QUEST     = Quest(7499,"7499_tapoy_occupation_change","village_master")
CREATED   = State('Start',     QUEST)
STARTED   = State('Started',   QUEST)
COMPLETED = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(7499)

QUEST.addTalkId(7499)