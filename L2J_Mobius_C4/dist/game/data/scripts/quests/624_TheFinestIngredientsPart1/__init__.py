# by disKret
import sys
from com.l2jmobius import Config 
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

#NPC
JEREMY = 8521

#ITEMS
TRUNK_OF_NEPENTHES,FOOT_OF_BANDERSNATCHLING,SECRET_SPICE,SAUCE=range(7202,7206)
CRYOLITE=7080

#MOBS
MOBS = HOT_SPRINGS_ATROX,HOT_SPRINGS_ATROXSPAWN,HOT_SPRINGS_BANDERSNATCHLING,HOT_SPRINGS_NEPENTHES = 1321,1317,1314,1319
ITEMS={
    HOT_SPRINGS_ATROX:SECRET_SPICE,
    HOT_SPRINGS_ATROXSPAWN:SECRET_SPICE,
    HOT_SPRINGS_BANDERSNATCHLING:FOOT_OF_BANDERSNATCHLING,
    HOT_SPRINGS_NEPENTHES:TRUNK_OF_NEPENTHES
}

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [TRUNK_OF_NEPENTHES, FOOT_OF_BANDERSNATCHLING, SECRET_SPICE]

 def onEvent (self,event,st) :
   cond = st.getInt("cond")
   htmltext = event
   trunk = st.getQuestItemsCount(TRUNK_OF_NEPENTHES)
   foot = st.getQuestItemsCount(FOOT_OF_BANDERSNATCHLING)
   spice = st.getQuestItemsCount(SECRET_SPICE)
   if event == "8521-1.htm" :
     if st.getPlayer().getLevel() >= 73 : 
        st.set("cond","1")
        st.setState(STARTED)
        st.playSound("ItemSound.quest_accept")       
     else:
        htmltext = "8521-0a.htm"
        st.exitQuest(1)
   elif event == "8521-4.htm" :
     if trunk==foot==spice==50 :
       st.takeItems(TRUNK_OF_NEPENTHES,-1)
       st.takeItems(FOOT_OF_BANDERSNATCHLING,-1)
       st.takeItems(SECRET_SPICE,-1)
       st.playSound("ItemSound.quest_finish")
       st.giveItems(SAUCE,1)
       st.giveItems(CRYOLITE,1)
       htmltext = "8521-4.htm"
       st.exitQuest(1)
     else:
       htmltext="8521-5.htm"
       st.set("cond","1")
   return htmltext

 def onTalk (self,npc,st):
   htmltext = "<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>"
   npcId = npc.getNpcId()
   cond = st.getInt("cond")
   if cond == 0 :
      htmltext = "8521-0.htm"
   elif st.getState() == STARTED:
       if cond != 3 :
          htmltext = "8521-2.htm"
       else :
          htmltext = "8521-3.htm"
   return htmltext

 def onKill(self,npc,player,isPet):
   partyMember = self.getRandomPartyMember(player,"1")
   if not partyMember: return
   st = partyMember.getQuestState("624_TheFinestIngredientsPart1")
   if st :
     if st.getState() == STARTED :
       npcId = npc.getNpcId()
       if st.getInt("cond") == 1:
        numItems,chance = divmod(100*Config.RATE_DROP_QUEST,100)
        if st.getRandom(100) <chance :
          numItems = numItems + 1
        numItems = int(numItems)
        item = ITEMS[npcId]
        count = st.getQuestItemsCount(item)
        if count < 50 :
          if count + numItems > 50 :
            numItems = 50 - count
          st.giveItems(item,numItems)
          count_trunk = st.getQuestItemsCount(TRUNK_OF_NEPENTHES)
          count_foot = st.getQuestItemsCount(FOOT_OF_BANDERSNATCHLING)
          count_spice = st.getQuestItemsCount(SECRET_SPICE)
          if count_trunk == count_foot == count_spice == 50 :
            st.set("cond","3")
            st.playSound("ItemSound.quest_middle")
          else:
            st.playSound("ItemSound.quest_itemget")  
   return

QUEST       = Quest(624,"624_TheFinestIngredientsPart1","The Finest Ingredients - Part 1")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(JEREMY)
QUEST.addTalkId(JEREMY)

for i in MOBS :
  QUEST.addKillId(i)