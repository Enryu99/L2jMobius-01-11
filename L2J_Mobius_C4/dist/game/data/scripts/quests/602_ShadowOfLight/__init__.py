# by disKret (adapted for L2JLisvus by roko91)

import sys
from com.l2jmobius import Config
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

qn = "602_ShadowOfLight"

#NPC
EYE_OF_ARGOS = 8683
#ITEMS
EYE_OF_DARKNESS = 7189
#CHANCE
CHANCE = {
    1304:50,
    1299:45
}
REWARDS = [[6699,40000,120000,20000,0,19],[6698,60000,110000,15000,20,39],[6700,40000,150000,10000,40,49],[0,100000,140000,11250,50,100]]

#MOBS
MOBS = [ 1299,1304 ]

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [EYE_OF_DARKNESS]

 def onEvent (self,event,st) :
   cond = st.getInt("cond")
   htmltext = event
   if event == "8683-1.htm" :
     if st.getPlayer().getLevel() >= 68 : 
        st.set("cond","1")
        st.setState(STARTED)
        st.playSound("ItemSound.quest_accept")
     else :
        htmltext = "8683-0a.htm"
        st.exitQuest(1)
   if event == "8683-4.htm" :
     if st.getQuestItemsCount(EYE_OF_DARKNESS) == 100 :
        random = st.getRandom(100)
        i = 0
        while i < len(REWARDS) :
            item,adena,exp,sp,chance,chance2=REWARDS[i]
            if chance<=random<= chance2 :
              break
            i = i+1
        st.giveItems(57,adena)
        if item :
           st.giveItems(item,3)
        st.addExpAndSp(exp,sp)
        st.takeItems(EYE_OF_DARKNESS,-1)
        st.playSound("ItemSound.quest_finish")
        st.exitQuest(1)
     else :
        htmltext = "8683-4a.htm"
   return htmltext

 def onTalk (self,npc,st):
   htmltext = "<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>"
   if st :
        cond = st.getInt("cond")
        if cond == 0 :
          htmltext = "8683-0.htm"
        elif cond == 1 :
          htmltext = "8683-2.htm"
        elif cond == 2 :
          htmltext = "8683-3.htm"
   return htmltext

 def onKill(self,npc,player,isPet):
     partyMember = self.getRandomPartyMember(player,"1")
     if not partyMember: return
     st = partyMember.getQuestState(qn)
     if st :
        if st.getState() == STARTED :  
           count = st.getQuestItemsCount(EYE_OF_DARKNESS)
           chance = CHANCE[npc.getNpcId()]*Config.RATE_DROP_QUEST
           numItems, chance = divmod(chance,100)
           if st.getInt("cond") == 1 :
             if st.getRandom(100) < chance :
                 numItems = numItems + 1
             if count+numItems>=100 :
                numItems =100-count
                st.playSound("ItemSound.quest_middle")
                st.set("cond","2")
             else :
                st.playSound("ItemSound.quest_itemget")
             st.giveItems(EYE_OF_DARKNESS,int(numItems))
     return

QUEST       = Quest(602,qn,"Shadow Of Light")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(EYE_OF_ARGOS)
QUEST.addTalkId(EYE_OF_ARGOS)

for i in MOBS :
  QUEST.addKillId(i)