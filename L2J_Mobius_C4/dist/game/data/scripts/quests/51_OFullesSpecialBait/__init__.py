# Contributed by Kilkenny to the Official L2J Datapack Project.
# with little cleanups by DrLecter(adapted for L2JLisvus by roko91).
# Visit http://www.l2jdp.com/trac if you find a bug.

import sys
from com.l2jmobius import Config
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

qn = "51_OFullesSpecialBait"

#NPC
OFULLE = 8572
#ITEMS
LOST_BAIT = 7622
#REWARDS
ICY_AIR_LURE = 7611
#MOB
FETTERED_SOUL = 552

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [LOST_BAIT]

 def onEvent (self,event,st) :
   htmltext = event
   if event == "8572-03.htm" :
     st.set("cond","1")
     st.setState(STARTED)
     st.playSound("ItemSound.quest_accept")
   elif event == "8572-07.htm" and st.getQuestItemsCount(LOST_BAIT) == 100 :
     htmltext = "8572-06.htm"
     st.giveItems(ICY_AIR_LURE,4)
     st.takeItems(LOST_BAIT,-1)
     st.playSound("ItemSound.quest_finish")
     st.unset("cond")
     st.setState(COMPLETED)
   return htmltext

 def onTalk (self,npc,st):
   htmltext = "<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>"
   npcId = npc.getNpcId()
   id = st.getState()
   cond = st.getInt("cond")
   if id == COMPLETED :
      htmltext = "<html><body>This quest has already been completed.</body></html>"
   elif cond == 0 :
      if st.getPlayer().getLevel() >= 36 :
         htmltext = "8572-01.htm"
      else:
         htmltext = "8572-02.htm"
         st.exitQuest(1)
   elif id == STARTED :
      if st.getQuestItemsCount(LOST_BAIT) == 100 :
         htmltext = "8572-04.htm"
      else :
         htmltext = "8572-05.htm"
   return htmltext

 def onKill(self,npc,player,isPet):
   partyMember = self.getRandomPartyMember(player,"1")
   if not partyMember : return
   st = partyMember.getQuestState(qn)
   if st :
      count = st.getQuestItemsCount(LOST_BAIT)
      if st.getInt("cond") == 1 and count < 100 :
         chance = 33 * Config.RATE_DROP_QUEST
         numItems, chance = divmod(chance,100)
         if st.getRandom(100) < chance : 
            numItems += 1
         if numItems :
            if count + numItems >= 100 :
               numItems = 100 - count
               st.playSound("ItemSound.quest_middle")
               st.set("cond","2")
            else:
               st.playSound("ItemSound.quest_itemget")
            st.giveItems(LOST_BAIT,1)
   return

QUEST       = Quest(51,qn,"O'Fulle's Special Bait")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(OFULLE)
QUEST.addTalkId(OFULLE)

QUEST.addKillId(FETTERED_SOUL)