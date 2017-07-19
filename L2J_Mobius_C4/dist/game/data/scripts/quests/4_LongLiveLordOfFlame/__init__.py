# Made by Mr. - Version 0.3 by DrLecter
import sys
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

HONEY_KHANDAR,BEAR_FUR_CLOAK,BLOODY_AXE,ANCESTOR_SKULL,SPIDER_DUST,DEEP_SEA_ORB = range(1541,1547)
NPC_GIFTS = {7585:BEAR_FUR_CLOAK,7566:HONEY_KHANDAR,7562:BLOODY_AXE,7560:ANCESTOR_SKULL,7559:SPIDER_DUST,7587:DEEP_SEA_ORB}

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = range(1541,1547)

 def onEvent (self,event,st) :
    htmltext = event
    if event == "7578-03.htm" :
      st.set("cond","1")
      st.set("id","1")
      st.setState(STARTED)
      st.playSound("ItemSound.quest_accept")
    return htmltext

 def onTalk (Self,npc,st):
   htmltext = "<html><body>I have nothing to say to you.</body></html>"
   npcId = npc.getNpcId()
   id = st.getState()
   cond = st.getInt("cond")
   if id == COMPLETED :
     htmltext = "<html><body>This quest has already been completed.</body></html>"
   elif npcId == 7578 :
     if cond == 0 :
       if st.getPlayer().getRace().ordinal() != 3 :
         htmltext = "7578-00.htm"
         st.exitQuest(1)
       elif st.getPlayer().getLevel() >= 2 :
         htmltext = "7578-02.htm"
       else:
         htmltext = "7578-01.htm"
         st.exitQuest(1)
     elif cond == 1 :
       htmltext = "7578-04.htm"
     elif cond == 2 :
       htmltext = "7578-06.htm"
       st.giveItems(4,1)
       for item in NPC_GIFTS.values():
           st.takeItems(item,-1)
       st.unset("cond")
       st.setState(COMPLETED)
       st.playSound("ItemSound.quest_finish")
   elif npcId in NPC_GIFTS.keys() and cond == 1 :
     item=NPC_GIFTS[npcId]
     npc=str(npcId)
     if st.getQuestItemsCount(item) :
       htmltext = npc+"-02.htm"
     else :
       st.giveItems(item,1)
       htmltext = npc+"-01.htm"
       count = 0
       for item in NPC_GIFTS.values():
         count += st.getQuestItemsCount(item)
       if count == 6 :
         st.set("cond","2")
         st.set("id","2")
         st.playSound("ItemSound.quest_middle")
       else :
         st.playSound("ItemSound.quest_itemget")
   return htmltext

QUEST     = Quest(4,"4_LongLiveLordOfFlame","Long Live the Paagrio Lord")
CREATED   = State('Start',     QUEST)
STARTING  = State('Starting',  QUEST)
STARTED   = State('Started',   QUEST)
COMPLETED = State('Completed', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(7578)

QUEST.addTalkId(7578)

QUEST.addTalkId(7559)
QUEST.addTalkId(7560)
QUEST.addTalkId(7562)
QUEST.addTalkId(7566)
QUEST.addTalkId(7578)
QUEST.addTalkId(7585)
QUEST.addTalkId(7587)