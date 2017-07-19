#Made by Emperorc (adapted for L2JLisvus by roko91)

import sys
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

qn = "614_SlayTheEnemyCommander_Varka"

#NPC
Ashas = 8377
Tayr = 10302

#Quest Items
Tayr_Head = 7241
Wisdom_Feather = 7230
Varka_Alliance_Four = 7224

def giveReward(st,npc):
    if st.getState() == STARTED :
        npcId = npc.getNpcId()
        cond = st.getInt("cond")
        if npcId == Tayr :
            if st.getPlayer().isAlliedWithVarka() :
                if cond == 1:
                    if st.getPlayer().getAllianceWithVarkaKetra() == -4 and st.getQuestItemsCount(Varka_Alliance_Four) :
                        st.giveItems(Tayr_Head,1)
                        st.set("cond","2")

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [Tayr_Head]

 def onEvent (self,event,st) :
   htmltext = event
   if event == "8377-04.htm" :
       if st.getPlayer().getAllianceWithVarkaKetra() == -4 and st.getQuestItemsCount(Varka_Alliance_Four) :
            if st.getPlayer().getLevel() >= 75 :
                    st.set("cond","1")
                    st.setState(STARTED)
                    st.playSound("ItemSound.quest_accept")
                    htmltext = "8377-04.htm"
            else :
                htmltext = "8377-03.htm"
                st.exitQuest(1)
       else :
            htmltext = "8377-02.htm"
            st.exitQuest(1)
   elif event == "8377-07.htm" :
       st.takeItems(Tayr_Head,-1)
       st.giveItems(Wisdom_Feather,1)
       st.addExpAndSp(10000,0)
       st.playSound("ItemSound.quest_finish")
       htmltext = "8377-07.htm"
       st.exitQuest(1)
   return htmltext

 def onTalk (self,npc,st):
    htmltext = "<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>"
    if st :
      npcId = npc.getNpcId()
      cond = st.getInt("cond")
      Head = st.getQuestItemsCount(Tayr_Head)
      Wisdom = st.getQuestItemsCount(Wisdom_Feather)
      if npcId == Ashas :
          if Wisdom == 0 :
              if Head == 0:
                  if cond != 1 :
                      htmltext = "8377-01.htm"
                  else:
                      htmltext = "8377-06.htm"
              else :
                  htmltext = "8377-05.htm"
          #else:
              #htmltext="<html><body>This quest has already been completed</body></html>"
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

QUEST       = Quest(614,qn,"Slay The Enemy Commander!")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(Ashas)
QUEST.addTalkId(Ashas)

QUEST.addKillId(Tayr)