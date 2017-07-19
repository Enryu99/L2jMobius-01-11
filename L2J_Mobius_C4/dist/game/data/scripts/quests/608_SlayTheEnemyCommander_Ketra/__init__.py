#Made by Emperorc (adapted for L2JLisvus by roko91)

import sys
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

qn = "608_SlayTheEnemyCommander_Ketra"

#NPC
Kadun = 8370
Mos = 10312

#Quest Items
Mos_Head = 7236
Wisdom_Totem = 7220
Ketra_Alliance_Four = 7214

def giveReward(st,npc):
    if st.getState() == STARTED :
        npcId = npc.getNpcId()
        cond = st.getInt("cond")
        if npcId == Mos :
            if st.getPlayer().isAlliedWithKetra() :
                if cond == 1:
                    if st.getPlayer().getAllianceWithVarkaKetra() == 4 and st.getQuestItemsCount(Ketra_Alliance_Four) :
                        st.giveItems(Mos_Head,1)
                        st.set("cond","2")

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [Mos_Head]

 def onEvent (self,event,st) :
   htmltext = event
   if event == "8370-04.htm" :
       if st.getPlayer().getAllianceWithVarkaKetra() == 4 and st.getQuestItemsCount(Ketra_Alliance_Four) :
            if st.getPlayer().getLevel() >= 75 :
                    st.set("cond","1")
                    st.setState(STARTED)
                    st.playSound("ItemSound.quest_accept")
                    htmltext = "8370-04.htm"
            else :
                htmltext = "8370-03.htm"
                st.exitQuest(1)
       else :
            htmltext = "8370-02.htm"
            st.exitQuest(1)
   elif event == "8370-07.htm" :
       st.takeItems(Mos_Head,-1)
       st.giveItems(Wisdom_Totem,1)
       st.addExpAndSp(10000,0)
       st.playSound("ItemSound.quest_finish")
       htmltext = "8370-07.htm"
       st.exitQuest(1)
   return htmltext

 def onTalk (self,npc,st):
    htmltext = "<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>"
    if st :
      npcId = npc.getNpcId()
      cond = st.getInt("cond")
      Head = st.getQuestItemsCount(Mos_Head)
      Wisdom = st.getQuestItemsCount(Wisdom_Totem)
      if npcId == Kadun :
          if Wisdom == 0 :
              if Head == 0:
                  if cond != 1 :
                      htmltext = "8370-01.htm"
                  else:
                      htmltext = "8370-06.htm"
              else :
                  htmltext = "8370-05.htm"
          #else:
              #htmltext="<html><body>This quest has already been completed.</body></html>"
    return htmltext

 def onKill(self,npc,player,isPet):
    partyMembers = [player]
    party = player.getParty()
    if party :
       partyMembers = party.getPartyMembers().toArray()
       for player in partyMembers :
           pst = player.getQuestState(qn)
           if pst :
              giveReward(pst,npc)
    else :
       pst = player.getQuestState(qn)
       if pst :
          giveReward(pst,npc)
    return

QUEST       = Quest(608,qn,"Slay The Enemy Commander!")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(Kadun)
QUEST.addTalkId(Kadun)

QUEST.addKillId(Mos)