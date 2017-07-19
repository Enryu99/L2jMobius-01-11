# Made by Mr. - Version 0.3 by DrLecter
import sys
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

FLOATING_STONE = 1492
RING_OF_FIREFLY = 1509
ADENA = 57

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [FLOATING_STONE]

 def onEvent (self,event,st) :
    htmltext = event
    if event == "7536-03.htm" :
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
     if st.getPlayer().getLevel() >= 11 :
       htmltext = "7536-02.htm"
     else:
       htmltext = "7536-01.htm"
       st.exitQuest(1)
   else:
     if st.getQuestItemsCount(FLOATING_STONE)<50 :
       htmltext = "7536-04.htm"
     else :
       if st.getQuestItemsCount(RING_OF_FIREFLY)==0 :
          htmltext = "7536-05.htm"
          st.giveItems(RING_OF_FIREFLY,1)
       else :
          htmltext = "7536-06.htm"
          st.giveItems(ADENA,2400)
       st.addExpAndSp(0,60)
       st.playSound("ItemSound.quest_finish")
       st.takeItems(FLOATING_STONE,-1)
       st.exitQuest(1)
   return htmltext

 def onKill (self,npc,player,isPet):
   st = player.getQuestState("295_DreamsOfFlight")
   if st :
     if st.getState() != STARTED : return
     count=st.getQuestItemsCount(FLOATING_STONE)
     if count < 50 :
       if st.getRandom(100) < 25 and count < 49 :
         st.giveItems(FLOATING_STONE,2)
       else:
         st.giveItems(FLOATING_STONE,1)
       if count == 49 :
           st.playSound("ItemSound.quest_middle")
           st.set("cond","2")
       else:
           st.playSound("ItemSound.quest_itemget")
   return

QUEST       = Quest(295,"295_DreamsOfFlight","Dreams Of Flight")
CREATED     = State('Start', QUEST)
STARTING     = State('Starting', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)


QUEST.setInitialState(CREATED)
QUEST.addStartNpc(7536)

QUEST.addTalkId(7536)

QUEST.addKillId(153)