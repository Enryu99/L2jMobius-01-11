# Made by Mr. Have fun! - Version 0.3 by DrLecter
import sys
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

WOLF_PELT = 702
LEATHER_TUNIC = 429
LEATHER_CAP = 42
CLOTH_CAP = 41
HOSE = 462
LEATHER_SHIELD = 18

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [WOLF_PELT]

 def onEvent (self,event,st) :
    htmltext = event
    if event == "7001-03.htm" :
      st.set("cond","1")
      st.setState(STARTED)
      st.playSound("ItemSound.quest_accept")
    return htmltext

 def onTalk (Self,npc,st):
   htmltext = "<html><body>I have nothing to say to you.</body></html>"
   id = st.getState()
   if id == CREATED :
     st.set("cond","0")
   if st.getInt("cond")==0 :
     if st.getPlayer().getLevel() >= 3 :
       htmltext = "7001-02.htm"
     else:
       htmltext = "7001-01.htm"
       st.exitQuest(1)
   else :
     if st.getQuestItemsCount(WOLF_PELT) < 40 :
       htmltext = "7001-05.htm"
     else :
       st.takeItems(WOLF_PELT,-1)
       n = st.getRandom(16)
       if n == 0 :
         st.giveItems(LEATHER_TUNIC,1)
         st.playSound("ItemSound.quest_jackpot")
       elif n < 6 :
         st.giveItems(LEATHER_CAP,1)
       elif n < 9 :
         st.giveItems(CLOTH_CAP,1)
       elif n < 13 :
         st.giveItems(HOSE,1)
       else:
         st.giveItems(LEATHER_SHIELD,1)
       htmltext = "7001-06.htm"
       st.exitQuest(1)
       st.playSound("ItemSound.quest_finish")
   return htmltext

 def onKill (self,npc,player,isPet):
   st = player.getQuestState("258_BringWolfPelt1")
   if st :
     if st.getState() != STARTED : return
     count = st.getQuestItemsCount(WOLF_PELT)
     if count<40 :
       st.giveItems(WOLF_PELT,1)
       if count == 39 :
         st.playSound("ItemSound.quest_middle")
         st.set("cond","2")
       else:
         st.playSound("ItemSound.quest_itemget")
   return

QUEST       = Quest(258,"258_BringWolfPelt1","Bring Wolf Pelt1")
CREATED     = State('Start', QUEST)
STARTING    = State('Starting', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(7001)

QUEST.addTalkId(7001)

QUEST.addKillId(120)
QUEST.addKillId(442)