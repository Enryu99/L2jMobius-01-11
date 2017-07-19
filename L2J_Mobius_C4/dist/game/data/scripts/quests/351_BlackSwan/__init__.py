# Made by mtrix
import sys
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

ADENA = 57
ORDER_OF_GOSTA = 4296
LIZARD_FANG = 4297
BARREL_OF_LEAGUE = 4298
BILL_OF_IASON_HEINE = 4310
CHANCE = 80
CHANCE2 = 4
class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [ORDER_OF_GOSTA, BARREL_OF_LEAGUE, LIZARD_FANG]

 def onEvent (self,event,st) :
     htmltext = event
     amount = st.getQuestItemsCount(LIZARD_FANG)
     amount2 = st.getQuestItemsCount(BARREL_OF_LEAGUE)
     if event == "7916-03.htm" :
         st.setState(STARTED)
         st.set("cond","1")
         st.giveItems(ORDER_OF_GOSTA,1)
         st.playSound("ItemSound.quest_accept")
     elif event == "7969-02a.htm" :
         if amount:
             htmltext = "7969-02.htm"
             st.giveItems(ADENA,amount*20)
             st.takeItems(LIZARD_FANG,-1)
     elif event == "7969-03a.htm" :
         if amount2 :
             htmltext = "7969-03.htm"
             st.set("cond","2")
             st.giveItems(ADENA,amount2*3880)
             st.giveItems(BILL_OF_IASON_HEINE,amount2)
             st.takeItems(BARREL_OF_LEAGUE,-1)
     elif event == "7969-01.htm" :
         if st.getInt("cond")==2 :
             htmltext = "7969-04.htm"
     elif event == "5" :
         st.exitQuest(1)
         st.playSound("ItemSound.quest_finish")
     return htmltext

 def onTalk (Self,npc,st):
     npcId = npc.getNpcId()
     htmltext = "<html><body>I have nothing to say to you.</body></html>"
     id = st.getState()
     level = st.getPlayer().getLevel()
     cond = st.getInt("cond")
     if npcId==7916 :
         if id == CREATED :
             if level>=32 :
                 htmltext = "7916-01.htm"
             else :
                 htmltext = "7916-00.htm"
                 st.exitQuest(1)
         elif cond>=1 :
             htmltext = "7916-04.htm"
     elif npcId==7969 :
         if cond==1 :
             htmltext = "7969-01.htm"
         elif cond==2 :
             htmltext = "7969-04.htm"
     return htmltext

 def onKill (self,npc,player,isPet):
     st = player.getQuestState("351_BlackSwan")
     if st :
         if st.getState() != STARTED : return
         npcId = npc.getNpcId()
         cond = st.getInt("cond")
         random = st.getRandom(100)
         if random<=CHANCE :
             st.giveItems(LIZARD_FANG,st.getRandom(5)+1)
             st.playSound("ItemSound.quest_itemget")
         if random<=CHANCE2 :
             st.giveItems(BARREL_OF_LEAGUE,1)
     return

QUEST       = Quest(351,"351_BlackSwan","Black Swan")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(7916)

QUEST.addTalkId(7916)

QUEST.addTalkId(7969)

QUEST.addKillId(784)
QUEST.addKillId(785)
QUEST.addKillId(1639)
QUEST.addKillId(1640)
QUEST.addKillId(1642)
QUEST.addKillId(1643)