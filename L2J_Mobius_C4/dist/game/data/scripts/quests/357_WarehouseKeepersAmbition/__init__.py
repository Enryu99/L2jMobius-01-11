# Made by disKret
import sys
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

#CUSTOM VALUES
DROPRATE=50
REWARD1=900  #This is paid per item
REWARD2=10000  #Extra reward, if > 100

#NPC
SILVA = 7686

#ITEMS
JADE_CRYSTAL = 5867

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [JADE_CRYSTAL]

 def onEvent (self,event,st) :
   htmltext = event
   if event == "7686-2.htm" :
     st.set("cond","1")
     st.setState(STARTED)
     st.playSound("ItemSound.quest_accept")
   elif event == "7686-7.htm" :
     count = st.getQuestItemsCount(JADE_CRYSTAL)
     if count:
       reward = count * REWARD1
       if count >= 100 :
         reward = reward + REWARD2
       st.takeItems(JADE_CRYSTAL,-1)
       st.giveItems(57,reward)
     else:
       htmltext="7686-4.htm"
   if event == "7686-8.htm" :
     st.playSound("ItemSound.quest_finish")
     st.exitQuest(1)
   return htmltext

 def onTalk (Self,npc,st):
   htmltext = "<html><body>I have nothing to say to you.</body></html>"
   id = st.getState()
   cond=st.getInt("cond")
   jade = st.getQuestItemsCount(JADE_CRYSTAL)
   if cond == 0 :
     if st.getPlayer().getLevel() >= 47 :
       htmltext = "7686-0.htm"
     else:
       htmltext = "7686-0a.htm"
       st.exitQuest(1)
   elif not jade :
       htmltext = "7686-4.htm"
   elif jade :
       htmltext = "7686-6.htm"
   return htmltext

 def onKill (self,npc,player,isPet):
   partyMember = self.getRandomPartyMemberState(player,STARTED)
   if not partyMember: return
   st = partyMember.getQuestState("357_WarehouseKeepersAmbition")
   if st :
      chance = st.getRandom(100) 
      if chance < DROPRATE :
        st.giveItems(JADE_CRYSTAL,1)
        st.playSound("ItemSound.quest_itemget")	
   return

QUEST       = Quest(357,"357_WarehouseKeepersAmbition","Warehouse Keepers Ambition")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(SILVA)

QUEST.addTalkId(SILVA)

for MOBS in range(594,598) :
  QUEST.addKillId(MOBS)