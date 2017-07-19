#
# Created by DraX on 2005.08.08 modified by Ariakas on 2005.09.19
#

import sys

from com.l2jmobius.gameserver.model.quest        import State
from com.l2jmobius.gameserver.model.quest        import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

WAREHOUSE_CHIEF_REED = 7520

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onEvent (self,event,st):
   htmltext = "No Quest"
   Race     = st.getPlayer().getRace()
   ClassId  = st.getPlayer().getClassId()
   Level    = st.getPlayer().getLevel()
   if event == "7520-01.htm":
     return "7520-01.htm"
   if event == "7520-02.htm":
     return "7520-02.htm"
   if event == "7520-03.htm":
     return "7520-03.htm"
   if event == "7520-04.htm":
     return "7520-04.htm"
   st.setState(COMPLETED)
   st.exitQuest(1)
   return htmltext

 def onTalk (Self,npc,st):
   npcId = npc.getNpcId()
   Race    = st.getPlayer().getRace()
   ClassId = st.getPlayer().getClassId()
   # Dwarfs got accepted
   if npcId == WAREHOUSE_CHIEF_REED and Race in [Race.Dwarf]:
     if ClassId in [ClassId.dwarvenFighter]:
       htmltext = "7520-01.htm"
       st.setState(STARTED)
       return htmltext
     if ClassId in [ClassId.scavenger, ClassId.artisan]:
       htmltext = "7520-05.htm"
       st.setState(COMPLETED)
       st.exitQuest(1)
       return htmltext
     if ClassId in [ClassId.bountyHunter, ClassId.warsmith]:
       htmltext = "7520-06.htm"
       st.setState(COMPLETED)
       st.exitQuest(1)
       return htmltext
   # All other Races must be out
   if npcId == WAREHOUSE_CHIEF_REED and Race in [Race.Orc, Race.DarkElf, Race.Elf, Race.Human]:
     st.setState(COMPLETED)
     st.exitQuest(1)
     return "7520-07.htm"

QUEST   = Quest(7520,"7520_reed_occupation_change","village_master")
CREATED   = State('Start',     QUEST)
STARTED   = State('Started',   QUEST)
COMPLETED = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(7520)

QUEST.addTalkId(7520)